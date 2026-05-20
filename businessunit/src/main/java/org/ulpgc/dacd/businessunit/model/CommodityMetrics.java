package org.ulpgc.dacd.businessunit.model;

public record CommodityMetrics(
        String commodity,
        double latestClosePrice,
        double previousClosePrice,
        double priceChangePercent,
        double precipitation,
        double rootZoneSoilWetness,
        double temperatureMax,
        double temperatureMin,
        double priceVolatility,
        double priceTrend,
        double precipitationDelta,
        double soilWetnessDelta,
        double temperatureMaxDelta
) {
}