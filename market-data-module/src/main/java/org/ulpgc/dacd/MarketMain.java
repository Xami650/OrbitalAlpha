package org.ulpgc.dacd;

public class MarketMain {
    static void main() {
        AlphaVantageMarketFeeder feeder = new AlphaVantageMarketFeeder();
        SqliteMarketStore store = new SqliteMarketStore();
        store.initialize();

        MarketController controller = new MarketController(feeder, store);
        controller.start();
    }
}