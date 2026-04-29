package org.ulpgc.dacd.marketfeeder.model;

import java.time.Instant;

public record MarketEvent(
        Instant ts,
        String ss,
        String symbol,
        Instant priceTimestamp,
        double open,
        double high,
        double low,
        double close,
        double adjustedClose,
        long volume,
        double dividendAmount){

}