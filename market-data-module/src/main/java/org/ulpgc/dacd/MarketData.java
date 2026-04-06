package org.ulpgc.dacd;

public record MarketData(
        String symbol,
        String weekDate,
        double open,
        double high,
        double low,
        double close,
        double adjustedClose,
        long volume,
        double dividendAmount
) {}
