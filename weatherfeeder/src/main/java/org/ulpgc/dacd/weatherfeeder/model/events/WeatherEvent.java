package org.ulpgc.dacd.weatherfeeder.model.events;

import java.time.Instant;

// Wire-format event consumed by downstream modules (businessunit).
// Field names ts/ss are kept short on purpose: they are the JSON keys
// that consumers parse and renaming them would break the contract.
public record WeatherEvent(
        Instant ts,
        String ss,
        String producerId,
        String producerName,
        String commodityType,
        String date,
        double precipitation,
        double rootZoneSoilWetness,
        double temperatureMax,
        double temperatureMin
) {
}