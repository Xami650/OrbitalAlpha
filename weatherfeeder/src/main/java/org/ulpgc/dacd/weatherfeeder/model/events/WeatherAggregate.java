package org.ulpgc.dacd.weatherfeeder.model.events;

import java.time.Instant;

// Wire-format aggregate consumed by downstream modules (businessunit).
// Field names ts/ss are kept short on purpose: they are the JSON keys
// that consumers parse and renaming them would break the contract.
public record WeatherAggregate(
        Instant ts,
        String ss,
        String producerId,
        String producerName,
        String commodityType,
        String periodStart,
        String periodEnd,
        int daysUsed,
        double avgPrecipitation,
        double avgRootZoneSoilWetness,
        double avgTemperatureMax,
        double avgTemperatureMin
) {
}