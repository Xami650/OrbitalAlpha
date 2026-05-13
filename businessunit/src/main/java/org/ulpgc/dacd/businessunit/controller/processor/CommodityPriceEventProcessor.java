package org.ulpgc.dacd.businessunit.controller.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommodityPriceEventProcessor implements EventProcessor<CommodityPriceEventProcessor.PriceMetrics> {

    private static final String COMMODITY_PRICE_TOPIC = "CommodityPrice";

    @Override
    public Map<String, PriceMetrics> process(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = groupEventsByCommodity(historicalEvents);

        Map<String, PriceMetrics> priceMetricsByCommodity = new HashMap<>();

        eventsByCommodity.forEach((commodity, events) -> {
            List<JsonObject> orderedEvents = orderEventsByPriceTimestamp(events);

            if (orderedEvents.isEmpty()) {
                return;
            }

            JsonObject latest = orderedEvents.get(orderedEvents.size() - 1);
            JsonObject previous = orderedEvents.size() > 1
                    ? orderedEvents.get(orderedEvents.size() - 2)
                    : latest;

            double latestClose = readDouble(latest, "close");
            double previousClose = readDouble(previous, "close");
            double priceChangePercent = calculatePriceChangePercent(latestClose, previousClose);

            priceMetricsByCommodity.put(commodity, new PriceMetrics(
                    latestClose,
                    previousClose,
                    priceChangePercent
            ));
        });

        return priceMetricsByCommodity;
    }

    private Map<String, List<JsonObject>> groupEventsByCommodity(List<HistoricalEvent> historicalEvents) {
        Map<String, List<JsonObject>> eventsByCommodity = new HashMap<>();

        historicalEvents.stream()
                .filter(this::isCommodityPriceEvent)
                .map(this::parseJson)
                .filter(json -> json.has("symbol"))
                .forEach(json -> {
                    String commodity = json.get("symbol").getAsString().toUpperCase();
                    eventsByCommodity.computeIfAbsent(commodity, ignored -> new ArrayList<>()).add(json);
                });

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
            double priceChangePercent
    ) {
    }
}