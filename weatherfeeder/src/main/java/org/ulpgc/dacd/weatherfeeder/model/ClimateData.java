package org.ulpgc.dacd.weatherfeeder.model;

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
