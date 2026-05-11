package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

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
                .filter(json -> json.has("commodityType"))
                .forEach(json -> {
                    String commodity = mapCommodityTypeToSymbol(
                            json.get("commodityType").getAsString()
                    );

                    weatherMetricsByCommodity.put(commodity, new WeatherMetrics(
                            readDouble(json, "precipitation"),
                            readDouble(json, "rootZoneSoilWetness"),
                            readDouble(json, "temperatureMax"),
                            readDouble(json, "temperatureMin")
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
            case "SOYBEANS", "SOYBEAN", "SOY" -> "SOYB";
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
