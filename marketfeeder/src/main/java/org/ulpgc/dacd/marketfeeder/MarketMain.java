package org.ulpgc.dacd.marketfeeder;

import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.config.MarketFeederConfig;
import org.ulpgc.dacd.marketfeeder.config.MarketFeederConfigLoader;
import org.ulpgc.dacd.marketfeeder.controller.MarketController;
import org.ulpgc.dacd.marketfeeder.controller.feeder.AlphaVantageMarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.feeder.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.feeder.parser.AlphaVantageMarketParser;
import org.ulpgc.dacd.marketfeeder.controller.feeder.parser.MarketParser;
import org.ulpgc.dacd.marketfeeder.controller.publisher.ActiveMqEventPublisher;

@SuppressWarnings("UnnecessaryModifier")
public class MarketMain {

    private static final Logger logger = LoggerFactory.getLogger(MarketMain.class);
    private static final String CONFIG_FILE = "marketfeeder.properties";

    public static void main(String[] args) {
        logger.info("Starting MarketFeeder...");
        MarketFeederConfig config =
                new MarketFeederConfigLoader(CONFIG_FILE).load();
        logger.info("Configuration loaded. Symbols: {}", config.symbols());

        MarketController controller = createController(config);
        controller.start();
    }

    @NotNull
    private static MarketController createController(MarketFeederConfig config) {
        return new MarketController(
                createFeeder(config),
                createPublisher(config),
                config.symbols(),
                config.intervalHours(),
                config.pauseMs()
        );
    }

    private static MarketFeeder createFeeder(MarketFeederConfig config) {
        return new AlphaVantageMarketFeeder(
                config.apiKey(),
                createHttpClient(),
                createParser(config)
        );
    }

    private static OkHttpClient createHttpClient() {
        return new OkHttpClient();
    }

    private static MarketParser createParser(MarketFeederConfig config) {
        return new AlphaVantageMarketParser(config.maxWeeks());
    }

    private static ActiveMqEventPublisher createPublisher(MarketFeederConfig config) {
        return new ActiveMqEventPublisher(
                config.brokerUrl(),
                config.topic()
        );
    }
}