package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherEventContractTest {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final Gson gson = new Gson();

    private final WeatherEventProcessor processor = new WeatherEventProcessor();

    @Test
    void processesFullWeatherAggregateJsonWithAllFields() {
        Instant now = Instant.now();
        String fileDate = FILE_DATE.format(now);

        Map<String, Object> aggregate = Map.ofEntries(
                Map.entry("ts", now.toString()),
                Map.entry("ss", "weatherfeeder"),
                Map.entry("producerId", "WHEAT_1"),
                Map.entry("producerName", "Beauce - France"),
                Map.entry("commodityType", "WHEAT"),
                Map.entry("periodStart", fileDate),
                Map.entry("periodEnd", fileDate),
                Map.entry("daysUsed", 7),
                Map.entry("avgPrecipitation", 2.45),
                Map.entry("avgRootZoneSoilWetness", 0.60),
                Map.entry("avgTemperatureMax", 13.97),
                Map.entry("avgTemperatureMin", 5.24)
        );

        String json = gson.toJson(aggregate);
        HistoricalEvent event = new HistoricalEvent("Weather", "weatherfeeder", fileDate, json);

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(List.of(event));

        assertThat(result).containsKey("WEAT");
        WeatherEventProcessor.WeatherMetrics metrics = result.get("WEAT");
        assertThat(metrics.precipitation()).isEqualTo(2.45);
        assertThat(metrics.rootZoneSoilWetness()).isEqualTo(0.60);
        assertThat(metrics.temperatureMax()).isEqualTo(13.97);
        assertThat(metrics.temperatureMin()).isEqualTo(5.24);
    }

    @Test
    void processesMultipleCommodityTypesFromRealSchema() {
        Instant now = Instant.now();
        String fileDate = FILE_DATE.format(now);

        List<HistoricalEvent> events = List.of(
                realWeatherEvent("WHEAT_1", "WHEAT", fileDate, 2.45, 0.60, 13.97, 5.24),
                realWeatherEvent("CORN_1", "CORN", fileDate, 1.20, 0.52, 22.10, 10.80),
                realWeatherEvent("COFFEE_1", "COFFEE", fileDate, 0.40, 0.42, 29.80, 17.30)
        );

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(events);

        assertThat(result).containsKeys("WEAT", "CORN", "JO");
        assertThat(result.get("WEAT").precipitation()).isEqualTo(2.45);
        assertThat(result.get("CORN").precipitation()).isEqualTo(1.20);
        assertThat(result.get("JO").precipitation()).isEqualTo(0.40);
    }

    private HistoricalEvent realWeatherEvent(
            String producerId, String commodityType, String periodStart,
            double precip, double soil, double tempMax, double tempMin
    ) {
        Map<String, Object> aggregate = Map.ofEntries(
                Map.entry("ts", Instant.now().toString()),
                Map.entry("ss", "weatherfeeder"),
                Map.entry("producerId", producerId),
                Map.entry("producerName", "Test Location"),
                Map.entry("commodityType", commodityType),
                Map.entry("periodStart", periodStart),
                Map.entry("periodEnd", periodStart),
                Map.entry("daysUsed", 7),
                Map.entry("avgPrecipitation", precip),
                Map.entry("avgRootZoneSoilWetness", soil),
                Map.entry("avgTemperatureMax", tempMax),
                Map.entry("avgTemperatureMin", tempMin)
        );
        return new HistoricalEvent("Weather", "weatherfeeder", periodStart, gson.toJson(aggregate));
    }
}
