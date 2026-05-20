package org.ulpgc.dacd.businessunit.model.events;

public record HistoricalEvent(
        String topic,
        String sourceSystem,
        String fileDate,
        String rawJson
) {
}