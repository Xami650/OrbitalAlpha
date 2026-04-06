package org.ulpgc.dacd;

public record ClimateData(
        String locationId,
        String date,
        double precipitation,
        double rootZoneSoilWetness,
        double temperatureMax,
        double temperatureMin
) {}
