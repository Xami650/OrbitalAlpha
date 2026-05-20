package org.ulpgc.dacd.businessunit.controller.serving;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.controller.batch.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.predictor.HeuristicRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.processor.CommodityPriceEventProcessor;
import org.ulpgc.dacd.businessunit.controller.processor.WeatherEventProcessor;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ServingLayerTest {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final Gson gson = new Gson();

    private InMemoryBatchDatamart batchDatamart;
    private InMemoryServingDatamart servingDatamart;
    private ServingLayer servingLayer;

    @BeforeEach
    void setUp() {
        batchDatamart = new InMemoryBatchDatamart();
        servingDatamart = new InMemoryServingDatamart();
        servingLayer = new ServingLayer(
                batchDatamart,
                servingDatamart,
                new HeuristicRiskPredictor(),
                new CommodityPriceEventProcessor(),
                new WeatherEventProcessor()
        );
    }

    @Test
    void rebuildProducesRiskSnapshotsFromPriceAndWeatherEvents() {
        String today = FILE_DATE.format(Instant.now());
        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", today, 5.80, 5.43));
        batchDatamart.saveHistoricalEvent(weatherEvent("WHEAT", today, 0.3, 0.28, 34.0, 18.0));

        servingLayer.rebuild();

        List<CommodityRiskSnapshot> snapshots = servingDatamart.findAllRiskSnapshots();
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().commodity()).isEqualTo("WEAT");
        assertThat(snapshots.getFirst().riskScore()).isGreaterThan(0);
        assertThat(snapshots.getFirst().riskLevel()).isNotNull();
        assertThat(snapshots.getFirst().reason()).isNotBlank();
    }

    @Test
    void rebuildWithOnlyPriceEventsUsesEmptyWeatherDefaults() {
        String today = FILE_DATE.format(Instant.now());
        batchDatamart.saveHistoricalEvent(priceEvent("CORN", today, 4.50, 4.30));

        servingLayer.rebuild();

        List<CommodityRiskSnapshot> snapshots = servingDatamart.findAllRiskSnapshots();
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().commodity()).isEqualTo("CORN");
    }

    @Test
    void rebuildWithMultipleCommoditiesProducesOneSnapshotEach() {
        String today = FILE_DATE.format(Instant.now());
        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", today, 5.80, 5.43));
        batchDatamart.saveHistoricalEvent(priceEvent("CORN", today, 4.50, 4.30));
        batchDatamart.saveHistoricalEvent(weatherEvent("WHEAT", today, 2.0, 0.6, 25.0, 10.0));
        batchDatamart.saveHistoricalEvent(weatherEvent("CORN", today, 1.5, 0.5, 28.0, 12.0));

        servingLayer.rebuild();

        List<CommodityRiskSnapshot> snapshots = servingDatamart.findAllRiskSnapshots();
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots).extracting(CommodityRiskSnapshot::commodity)
                .containsExactlyInAnyOrder("WEAT", "CORN");
    }

    @Test
    void rebuildOverwritesPreviousSnapshots() {
        String today = FILE_DATE.format(Instant.now());
        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", today, 5.80, 5.43));

        servingLayer.rebuild();

        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", today, 5.80, 5.80));
        servingLayer.rebuild();

        assertThat(servingDatamart.findAllRiskSnapshots()).hasSize(1);
    }

    @Test
    void rebuildWithNoEventsProducesNoSnapshots() {
        servingLayer.rebuild();

        assertThat(servingDatamart.findAllRiskSnapshots()).isEmpty();
    }

    @Test
    void highRiskInputProducesHighRiskSnapshot() {
        String today = FILE_DATE.format(Instant.now());
        String yesterday = FILE_DATE.format(Instant.now().minusSeconds(86400));

        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", yesterday, 5.00, 5.00));
        batchDatamart.saveHistoricalEvent(priceEvent("WEAT", today, 6.00, 5.00));
        batchDatamart.saveHistoricalEvent(weatherEvent("WHEAT", today, 0.3, 0.2, 35.0, 1.0));

        servingLayer.rebuild();

        CommodityRiskSnapshot snapshot = servingDatamart.findRiskSnapshotByCommodity("WEAT").get();
        assertThat(snapshot.riskScore()).isGreaterThanOrEqualTo(40.0);
    }

    private HistoricalEvent priceEvent(String symbol, String fileDate, double close, double previousClose) {
        String priceTimestamp = LocalDate.parse(fileDate, FILE_DATE).atStartOfDay()
                .toInstant(ZoneOffset.UTC).toString();

        Map<String, Object> json = Map.of(
                "ts", Instant.now().toString(),
                "ss", "AlphaVantage",
                "symbol", symbol,
                "close", close,
                "priceTimestamp", priceTimestamp
        );
        return new HistoricalEvent("CommodityPrice", "AlphaVantage", fileDate, gson.toJson(json));
    }

    private HistoricalEvent weatherEvent(
            String commodityType, String fileDate,
            double precip, double soil, double tempMax, double tempMin
    ) {
        Map<String, Object> json = Map.ofEntries(
                Map.entry("ts", Instant.now().toString()),
                Map.entry("ss", "weatherfeeder"),
                Map.entry("producerId", commodityType + "_1"),
                Map.entry("producerName", "Test Location"),
                Map.entry("commodityType", commodityType),
                Map.entry("periodStart", fileDate),
                Map.entry("periodEnd", fileDate),
                Map.entry("daysUsed", 7),
                Map.entry("avgPrecipitation", precip),
                Map.entry("avgRootZoneSoilWetness", soil),
                Map.entry("avgTemperatureMax", tempMax),
                Map.entry("avgTemperatureMin", tempMin)
        );
        return new HistoricalEvent("Weather", "weatherfeeder", fileDate, gson.toJson(json));
    }

    private static class InMemoryBatchDatamart implements BatchDatamart {
        private final List<HistoricalEvent> events = new ArrayList<>();

        @Override
        public void initialize() { events.clear(); }

        @Override
        public void clear() { events.clear(); }

        @Override
        public void saveHistoricalEvent(HistoricalEvent event) { events.add(event); }

        @Override
        public void saveHistoricalEvents(List<HistoricalEvent> events) { this.events.addAll(events); }

        @Override
        public List<HistoricalEvent> findAllHistoricalEvents() { return List.copyOf(events); }
    }

    private static class InMemoryServingDatamart implements ServingDatamart {
        private final Map<String, CommodityRiskSnapshot> store = new LinkedHashMap<>();

        @Override
        public void initialize() { store.clear(); }

        @Override
        public void saveRiskSnapshot(CommodityRiskSnapshot snapshot) {
            store.put(snapshot.commodity().toUpperCase(), snapshot);
        }

        @Override
        public List<CommodityRiskSnapshot> findAllRiskSnapshots() { return List.copyOf(store.values()); }

        @Override
        public Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity) {
            return Optional.ofNullable(store.get(commodity.toUpperCase()));
        }
    }
}
