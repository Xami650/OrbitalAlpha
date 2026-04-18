package org.ulpgc.dacd.marketfeeder.model;

import java.time.Instant;

public record CommoditiesInfo(
        String symbol,
        Instant priceTimestamp,
        double open,
        double high,
        double low,
        double close,
        double adjustedClose,
        long volume,
        double dividendAmount
) {}
