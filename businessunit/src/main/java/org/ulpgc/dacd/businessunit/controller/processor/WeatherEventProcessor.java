package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherEventProcessor implements EventProcessor<WeatherEventProcessor.WeatherMetrics> {

    private static final String WEATHER_TOPIC = "Weather";

    @Override
    public Map<String, WeatherMetrics> process(List<HistoricalEvent> historicalEvents) {
        Map<String, WeatherMetrics> weatherMetricsByCommodity = new HashMap<>();

        historicalEvents.stream()
                .filter(this::isWeatherEvent)
                .map(this::parseJson)
                .filter(json -> json.has("commodityType") && json.has("date"))
                .collect(java.util.stream.Collectors.groupingBy(
                        json -> mapCommodityTypeToSymbol(json.get("commodityType").getAsString())
                ))
                .forEach((commodity, events) -> {
                    JsonObject latest = events.stream()
                            .max(Comparator.comparing(json -> json.get("date").getAsString()))
                            .orElseThrow();

                    weatherMetricsByCommodity.put(commodity, new WeatherMetrics(
                            readDouble(latest, "precipitation"),
                            readDouble(latest, "rootZoneSoilWetness"),
                            readDouble(latest, "temperatureMax"),
                            readDouble(latest, "temperatureMin")
                    ));
                });

        return weatherMetricsByCommodity;
    }

    private boolean isWeatherEvent(HistoricalEvent event) {
        return event.topic().equalsIgnoreCase(WEATHER_TOPIC);
    }

    private JsonObject parseJson(HistoricalEvent event) {
        return JsonParser.parseString(event.rawJson()).getAsJsonObject();
    }

    private double readDouble(JsonObject json, String fieldName) {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return 0.0;
        }

        return json.get(fieldName).getAsDouble();
    }

    private String mapCommodityTypeToSymbol(String commodityType) {
        return switch (commodityType.toUpperCase()) {
            case "WHEAT" -> "WEAT";
            case "CORN" -> "CORN";
            case "SOYBEANS", "SOYBEAN", "SOY", "SOY_BEANS" -> "SOYB";
            case "COFFEE" -> "JO";
            case "NATURAL_GAS", "NATURAL GAS", "GAS" -> "UNG";
            default -> commodityType.toUpperCase();
        };
    }

    public record WeatherMetrics(
            double precipitation,
            double rootZoneSoilWetness,
            double temperatureMax,
            double temperatureMin
    ) {
        public static WeatherMetrics empty() {
            return new WeatherMetrics(0.0, 0.0, 0.0, 0.0);
        }
    }
}
