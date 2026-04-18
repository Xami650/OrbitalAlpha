package org.ulpgc.dacd.marketfeeder.model.feeders;

public interface MarketFeeder {
    String fetchWeeklySeriesRaw(String symbol);
}
