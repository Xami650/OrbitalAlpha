package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import org.ulpgc.dacd.businessunit.model.Topics;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherEventProcessor implements EventProcessor<WeatherEventProcessor.WeatherMetrics> {

    private static final Logger logger = LoggerFactory.getLogger(WeatherEventProcessor.class);
    private static final int TEMPORAL_WINDOW_WEEKS = 52;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Map<String, WeatherMetrics> process(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = groupEventsByCommodity(historicalEvents);
        Map<String, WeatherMetrics> weatherMetricsByCommodity = new HashMap<>();

        eventsByCommodity.forEach((commodity, events) -> {
            double[] avgMetrics = computeHistoricalAverages(events);

            JsonObject latest = events.stream()
                    .filter(json -> json.has("periodStart"))
                    .max(Comparator.comparing(json -> json.get("periodStart").getAsString()))
                    .orElse(null);

            if (latest == null) {
                logger.warn("No dated weather events found for {}", commodity);
                return;
            }

            double precipitation = readDouble(latest, "avgPrecipitation");
            double rootZoneSoilWetness = readDouble(latest, "avgRootZoneSoilWetness");
            double temperatureMax = readDouble(latest, "avgTemperatureMax");
            double temperatureMin = readDouble(latest, "avgTemperatureMin");

            weatherMetricsByCommodity.put(commodity, new WeatherMetrics(
                    precipitation,
                    rootZoneSoilWetness,
                    temperatureMax,
                    temperatureMin,
                    precipitation - avgMetrics[0],
                    rootZoneSoilWetness - avgMetrics[1],
                    temperatureMax - avgMetrics[2]
            ));
        });

        logger.info("Processed weather metrics for {} commodities", weatherMetricsByCommodity.size());
        return weatherMetricsByCommodity;
    }

    private Map<String, List<JsonObject>> groupEventsByCommodity(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = new HashMap<>();
        String cutoffDate = LocalDate.now().minusWeeks(TEMPORAL_WINDOW_WEEKS).format(FILE_DATE_FORMAT);

        historicalEvents.stream()
                .filter(this::isWeatherEvent)
                .filter(event -> event.fileDate().compareTo(cutoffDate) >= 0)
                .map(this::parseJson)
                .filter(json -> json.has("commodityType"))
                .forEach(json -> {
                    String commodity = mapCommodityTypeToSymbol(json.get("commodityType").getAsString());
                    eventsByCommodity.computeIfAbsent(commodity, ignored -> new ArrayList<>()).add(json);
                });

        logger.debug("Temporal filter applied: only processing events from {} onward ({} weeks)",
                cutoffDate, TEMPORAL_WINDOW_WEEKS);
        return eventsByCommodity;
    }

    private double[] computeHistoricalAverages(List<JsonObject> events) {
        double sumPrecipitation = 0.0;
        double sumSoilWetness = 0.0;
        double sumTempMax = 0.0;
        int count = 0;

        for (JsonObject json : events) {
            sumPrecipitation += readDouble(json, "avgPrecipitation");
            sumSoilWetness += readDouble(json, "avgRootZoneSoilWetness");
            sumTempMax += readDouble(json, "avgTemperatureMax");
            count++;
        }

        if (count == 0) {
            return new double[]{0.0, 0.0, 0.0};
        }

        return new double[]{
                sumPrecipitation / count,
                sumSoilWetness / count,
                sumTempMax / count
        };
    }

    private boolean isWeatherEvent(HistoricalEvent event) {
        return event.topic().equalsIgnoreCase(Topics.WEATHER);
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
            double temperatureMin,
            double precipitationDelta,
            double soilWetnessDelta,
            double temperatureMaxDelta
    ) {
        public static WeatherMetrics empty() {
            return new WeatherMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
