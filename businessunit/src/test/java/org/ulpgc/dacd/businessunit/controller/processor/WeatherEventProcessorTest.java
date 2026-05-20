package org.ulpgc.dacd.businessunit.controller.processor;

import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherEventProcessorTest {

    private final WeatherEventProcessor processor = new WeatherEventProcessor();

    @Test
    void extractsWeatherMetricsForKnownCommodity() {
        List<HistoricalEvent> events = List.of(
                weatherEvent("WHEAT", "20260429", 2.5, 0.58, 21.0, 8.0)
        );

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(events);

        assertThat(result).containsKey("WEAT");
        WeatherEventProcessor.WeatherMetrics metrics = result.get("WEAT");
        assertThat(metrics.precipitation()).isEqualTo(2.5);
        assertThat(metrics.rootZoneSoilWetness()).isEqualTo(0.58);
        assertThat(metrics.temperatureMax()).isEqualTo(21.0);
        assertThat(metrics.temperatureMin()).isEqualTo(8.0);
    }

    @Test
    void selectsMostRecentEventByDateField() {
        List<HistoricalEvent> events = List.of(
                weatherEvent("WHEAT", "20260427", 0.0, 0.30, 35.0, 2.0),
                weatherEvent("WHEAT", "20260429", 5.0, 0.70, 18.0, 10.0),
                weatherEvent("WHEAT", "20260428", 1.0, 0.50, 25.0, 6.0)
        );

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(events);

        WeatherEventProcessor.WeatherMetrics metrics = result.get("WEAT");
        assertThat(metrics.precipitation()).isEqualTo(5.0);
        assertThat(metrics.temperatureMax()).isEqualTo(18.0);
    }

    @Test
    void mapsCommodityTypesToSymbolsCorrectly() {
        List<HistoricalEvent> events = List.of(
                weatherEvent("WHEAT", "20260429", 1.0, 0.5, 20.0, 8.0),
                weatherEvent("CORN", "20260429", 2.0, 0.6, 22.0, 9.0),
                weatherEvent("SOYBEANS", "20260429", 3.0, 0.7, 24.0, 10.0),
                weatherEvent("COFFEE", "20260429", 4.0, 0.8, 26.0, 11.0),
                weatherEvent("NATURAL_GAS", "20260429", 5.0, 0.9, 28.0, 12.0),
                weatherEvent("SOY_BEANS", "20260429", 1.5, 0.5, 22.0, 9.0)
        );

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(events);

        assertThat(result).containsKeys("WEAT", "CORN", "SOYB", "JO", "UNG");
        assertThat(result).containsOnlyKeys("WEAT", "CORN", "SOYB", "JO", "UNG");
    }

    @Test
    void ignoresNonWeatherEvents() {
        List<HistoricalEvent> events = List.of(
                new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260429",
                        "{\"symbol\":\"WEAT\",\"close\":22.0}"),
                weatherEvent("WHEAT", "20260429", 1.0, 0.5, 20.0, 8.0)
        );

        Map<String, WeatherEventProcessor.WeatherMetrics> result = processor.process(events);

        assertThat(result).containsOnlyKeys("WEAT");
    }

    @Test
    void returnsEmptyMapWhenNoEvents() {
        assertThat(processor.process(List.of())).isEmpty();
    }

    @Test
    void emptyMetricsHaveZeroValues() {
        WeatherEventProcessor.WeatherMetrics empty = WeatherEventProcessor.WeatherMetrics.empty();

        assertThat(empty.precipitation()).isEqualTo(0.0);
        assertThat(empty.rootZoneSoilWetness()).isEqualTo(0.0);
        assertThat(empty.temperatureMax()).isEqualTo(0.0);
        assertThat(empty.temperatureMin()).isEqualTo(0.0);
    }

    private HistoricalEvent weatherEvent(
            String commodityType, String periodStart,
            double precipitation, double soilWetness,
            double tempMax, double tempMin
    ) {
        String json = String.format(
                "{\"commodityType\":\"%s\",\"periodStart\":\"%s\",\"avgPrecipitation\":%s," +
                "\"avgRootZoneSoilWetness\":%s,\"avgTemperatureMax\":%s,\"avgTemperatureMin\":%s}",
                commodityType, periodStart, precipitation, soilWetness, tempMax, tempMin
        );
        return new HistoricalEvent("Weather", "weatherfeeder", periodStart, json);
    }
}
