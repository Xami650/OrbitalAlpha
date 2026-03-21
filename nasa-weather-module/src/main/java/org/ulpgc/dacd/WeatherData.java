package org.ulpgc.dacd;

public record WeatherData(
        String cropName,
        double latitude,
        double longitude,
        String date,
        double precipitation,
        double soilMoisture,
        double maxTemp,
        double minTemp
) {}
