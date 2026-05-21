package org.ulpgc.dacd.businessunit.controller;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.controller.batch.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.batch.BatchLayer;
import org.ulpgc.dacd.businessunit.controller.batch.EventStoreReader;
import org.ulpgc.dacd.businessunit.controller.predictor.HeuristicRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.processor.CommodityPriceEventProcessor;
import org.ulpgc.dacd.businessunit.controller.processor.WeatherEventProcessor;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;
import org.ulpgc.dacd.businessunit.controller.serving.ServingDatamart;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaPipelineIntegrationTest {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final Gson gson = new Gson();

    private InMemoryBatchDatamart batchDatamart;
    private InMemoryServingDatamart servingDatamart;
    private CommodityRiskService riskService;
    private BatchLayer batchLayer;
    private ServingLayer servingLayer;

    @BeforeEach
    void setUp() {
        String today = FILE_DATE.format(Instant.now());

        List<HistoricalEvent> storeEvents = List.of(
                priceEvent("WEAT", today, 5.80, 5.43),
                priceEvent("CORN", today, 4.50, 4.30),
                weatherEvent("WHEAT", today, 0.3, 0.28, 34.0, 18.0),
                weatherEvent("CORN", today, 2.0, 0.6, 25.0, 12.0)
        );

        EventStoreReader reader = () -> storeEvents;
        batchDatamart = new InMemoryBatchDatamart();
        servingDatamart = new InMemoryServingDatamart();

        batchLayer = new BatchLayer(reader, batchDatamart);
        servingLayer = new ServingLayer(
                batchDatamart,
                servingDatamart,
                new HeuristicRiskPredictor(),
                new CommodityPriceEventProcessor(),
                new WeatherEventProcessor()
        );
        riskService = new CommodityRiskService(servingDatamart);
    }

    @Test
    void fullPipelineProducesQueryableRiskSnapshots() {
        batchLayer.rebuild();
        servingLayer.rebuild();

        List<CommodityRiskSnapshot> risks = riskService.getAllRisks();

        assertThat(risks).hasSize(2);
        assertThat(risks).extracting(CommodityRiskSnapshot::commodity)
                .containsExactlyInAnyOrder("WEAT", "CORN");
    }

    @Test
    void fullPipelineSnapshotsHaveValidScoresAndReasons() {
        batchLayer.rebuild();
        servingLayer.rebuild();

        for (CommodityRiskSnapshot snapshot : riskService.getAllRisks()) {
            assertThat(snapshot.riskScore()).isBetween(0.0, 100.0);
            assertThat(snapshot.riskLevel()).isNotNull();
            assertThat(snapshot.reason()).isNotBlank();
        }
    }

    @Test
    void singleCommodityQueryReturnsCorrectResult() {
        batchLayer.rebuild();
        servingLayer.rebuild();

        Optional<CommodityRiskSnapshot> weat = riskService.getRiskByCommodity("WEAT");
        assertThat(weat).isPresent();
        assertThat(weat.get().commodity()).isEqualTo("WEAT");

        Optional<CommodityRiskSnapshot> unknown = riskService.getRiskByCommodity("UNKNOWN");
        assertThat(unknown).isEmpty();
    }

    @Test
    void rebuildIsIdempotent() {
        batchLayer.rebuild();
        servingLayer.rebuild();
        List<CommodityRiskSnapshot> first = riskService.getAllRisks();

        batchLayer.rebuild();
        servingLayer.rebuild();
        List<CommodityRiskSnapshot> second = riskService.getAllRisks();

        assertThat(second).hasSameSizeAs(first);
    }

    private HistoricalEvent priceEvent(String symbol, String fileDate, double close, double previousClose) {
        String priceTimestamp = java.time.LocalDate.parse(fileDate, FILE_DATE).atStartOfDay()
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

    private HistoricalEvent weatherEvent(String commodityType, String fileDate, double precip, double soil, double tempMax, double tempMin) {
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

        @Override public void initialize() { events.clear(); }
        @Override public void clear() { events.clear(); }
        @Override public void saveHistoricalEvent(HistoricalEvent event) { events.add(event); }
        @Override public void saveHistoricalEvents(List<HistoricalEvent> events) { this.events.addAll(events); }
        @Override public List<HistoricalEvent> findAllHistoricalEvents() { return List.copyOf(events); }
    }

    private static class InMemoryServingDatamart implements ServingDatamart {
        private final Map<String, CommodityRiskSnapshot> store = new LinkedHashMap<>();

        @Override public void initialize() { store.clear(); }
        @Override public void saveRiskSnapshot(CommodityRiskSnapshot snapshot) {
            store.put(snapshot.commodity().toUpperCase(), snapshot);
        }
        @Override public List<CommodityRiskSnapshot> findAllRiskSnapshots() { return List.copyOf(store.values()); }
        @Override public Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity) {
            return Optional.ofNullable(store.get(commodity.toUpperCase()));
        }
    }
}
