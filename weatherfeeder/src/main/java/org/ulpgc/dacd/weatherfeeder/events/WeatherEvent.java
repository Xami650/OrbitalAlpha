package org.ulpgc.dacd.weatherfeeder.events;

public record WeatherEvent(
        String ts,
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
