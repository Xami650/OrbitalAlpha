package org.ulpgc.dacd;

public class MarketMain {
    static void main() {
        AlphaVantageMarketFeeder feeder = new AlphaVantageMarketFeeder();
        MarketStore store = new SqliteMarketStore();
        store.initialize();

        MarketController controller = new MarketController(feeder, store);
        controller.start();
    }
}