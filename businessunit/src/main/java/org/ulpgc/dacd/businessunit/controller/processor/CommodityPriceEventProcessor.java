package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommodityPriceEventProcessor implements EventProcessor<CommodityPriceEventProcessor.PriceMetrics> {

    private static final Logger logger = LoggerFactory.getLogger(CommodityPriceEventProcessor.class);

    private static final String COMMODITY_PRICE_TOPIC = "CommodityPrice";
    private static final int LOOKBACK_WINDOW = 8;
    private static final int TEMPORAL_WINDOW_WEEKS = 52;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Map<String, PriceMetrics> process(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = groupEventsByCommodity(historicalEvents);

        Map<String, PriceMetrics> priceMetricsByCommodity = new HashMap<>();

        eventsByCommodity.forEach((commodity, events) -> {
            List<JsonObject> orderedEvents = orderEventsByPriceTimestamp(events);

            if (orderedEvents.isEmpty()) {
                logger.warn("No ordered price events found for {}", commodity);
                return;
            }

            List<Double> allChanges = computeAllPriceChanges(orderedEvents);

            JsonObject latest = orderedEvents.getLast();
            JsonObject previous = orderedEvents.size() > 1
                    ? orderedEvents.get(orderedEvents.size() - 2)
                    : latest;

            double latestClose = readDouble(latest, "close");
            double previousClose = readDouble(previous, "close");
            double priceChangePercent = calculatePriceChangePercent(latestClose, previousClose);

            double priceVolatility = 0.0;
            double priceTrend = 0.0;

            if (!allChanges.isEmpty()) {
                List<Double> window = allChanges.subList(
                        Math.max(0, allChanges.size() - LOOKBACK_WINDOW),
                        allChanges.size()
                );
                if (window.size() >= 2) {
                    priceVolatility = calculateStdDev(window);
                    priceTrend = calculateMean(window);
                } else {
                    priceTrend = window.getFirst();
                }
            }

            priceMetricsByCommodity.put(commodity, new PriceMetrics(
                    latestClose,
                    previousClose,
                    priceChangePercent,
                    priceVolatility,
                    priceTrend
            ));
        });

        logger.info("Processed price metrics for {} commodities", priceMetricsByCommodity.size());
        return priceMetricsByCommodity;
    }

    private List<Double> computeAllPriceChanges(List<JsonObject> orderedEvents) {
        List<Double> changes = new ArrayList<>();
        for (int i = 1; i < orderedEvents.size(); i++) {
            double prev = readDouble(orderedEvents.get(i - 1), "close");
            double curr = readDouble(orderedEvents.get(i), "close");
            if (prev != 0.0) {
                changes.add(((curr - prev) / prev) * 100.0);
            }
        }
        return changes;
    }

    private double calculateMean(List<Double> values) {
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    private double calculateStdDev(List<Double> values) {
        double mean = calculateMean(values);
        double sumSquares = 0.0;
        for (double v : values) sumSquares += (v - mean) * (v - mean);
        return Math.sqrt(sumSquares / values.size());
    }

    private Map<String, List<JsonObject>> groupEventsByCommodity(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = new HashMap<>();
        String cutoffDate = LocalDate.now().minusWeeks(TEMPORAL_WINDOW_WEEKS).format(FILE_DATE_FORMAT);

        historicalEvents.stream()
                .filter(this::isCommodityPriceEvent)
                .filter(event -> event.fileDate().compareTo(cutoffDate) >= 0)
                .map(this::parseJson)
                .filter(json -> json.has("symbol"))
                .forEach(json -> {
                    String commodity = json.get("symbol").getAsString().toUpperCase();
                    eventsByCommodity.computeIfAbsent(commodity, ignored -> new ArrayList<>()).add(json);
                });

        logger.debug("Temporal filter applied: only processing events from {} onward ({} weeks)",
                cutoffDate, TEMPORAL_WINDOW_WEEKS);
        return eventsByCommodity;
    }

    private boolean isCommodityPriceEvent(HistoricalEvent event) {
        return event.topic().equalsIgnoreCase(COMMODITY_PRICE_TOPIC);
    }

    private List<JsonObject> orderEventsByPriceTimestamp(List<JsonObject> events) {
        return events.stream()
                .filter(json -> json.has("priceTimestamp"))
                .sorted(Comparator.comparing(json -> json.get("priceTimestamp").getAsString()))
                .toList();
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

    private double calculatePriceChangePercent(double latestClose, double previousClose) {
        if (previousClose == 0.0) {
            return 0.0;
        }

        return ((latestClose - previousClose) / previousClose) * 100.0;
    }

    public record PriceMetrics(
            double latestClosePrice,
            double previousClosePrice,
            double priceChangePercent,
            double priceVolatility,
            double priceTrend
    ) {
    }
}
