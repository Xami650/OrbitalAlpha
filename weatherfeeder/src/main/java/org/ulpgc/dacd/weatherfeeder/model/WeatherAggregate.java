package org.ulpgc.dacd.weatherfeeder.model;

import java.time.Instant;

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