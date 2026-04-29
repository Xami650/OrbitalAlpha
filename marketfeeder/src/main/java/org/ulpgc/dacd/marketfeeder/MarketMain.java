package org.ulpgc.dacd.marketfeeder;

import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.ulpgc.dacd.marketfeeder.controller.MarketController;
import org.ulpgc.dacd.marketfeeder.controller.feeders.AlphaVantageMarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.feeders.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.feeders.parsers.AlphaVantageMarketParser;
import org.ulpgc.dacd.marketfeeder.controller.feeders.parsers.MarketParser;
import org.ulpgc.dacd.marketfeeder.controller.publisher.ActiveMqEventPublisher;


import java.util.Arrays;
import java.util.List;

@SuppressWarnings("UnnecessaryModifier")
public class MarketMain {

    public static void main(String[] args) {
        String apiKey = System.getenv("MARKET_API_KEY");
        if (apiKey == null || apiKey.isBlank() || args.length < 1) {
            System.out.println("Usage: java MarketMain <symbols_comma_separated> [intervalHours] [maxWeeks] [pauseMs]");
            System.out.println("Example: java MarketMain WEAT,CORN,SOYB,JO,UNG 24 520 15000");
            System.out.println("Supported commodity ETFs initially considered: WEAT (wheat), CORN (corn), SOYB (soybeans), JO (coffee), UNG (natural gas)");
            System.out.println("Required environment variables:");
            System.out.println("  MARKET_API_KEY");
            System.out.println("pauseMs defaults to 15000 ms due to API rate limiting.");
            return;
        }

        MarketController controller = getMarketController(args, apiKey);
        controller.start();
    }

    @NotNull
    private static MarketController getMarketController(String[] args, String apiKey) {
        List<String> symbols = Arrays.asList(args[0].split(","));
        int intervalHours = args.length > 1 ? Integer.parseInt(args[1]) : 24;
        int maxWeeks = args.length > 2 ? Integer.parseInt(args[2]) : 520;
        long pauseMs = args.length > 3 ? Long.parseLong(args[3]) : 15000L;

        OkHttpClient client = new OkHttpClient();
        MarketParser parser = new AlphaVantageMarketParser(maxWeeks);
        MarketFeeder feeder = new AlphaVantageMarketFeeder(apiKey, client, parser);
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher("tcp://localhost:61616", "CommodityPrice");
        return new MarketController(feeder, publisher, symbols, intervalHours, pauseMs);
    }
}
