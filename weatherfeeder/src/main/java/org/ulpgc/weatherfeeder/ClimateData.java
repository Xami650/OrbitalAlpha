package org.ulpgc.weatherfeeder;

public record ClimateData(
        String producerId,
        String producerName,
        String commodityType,
        String date,
        double precipitation,
        double rootZoneSoilWetness,
        double temperatureMax,
        double temperatureMin
) {}
