package org.ulpgc.dacd.marketfeeder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;
import org.ulpgc.dacd.marketfeeder.model.feeders.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.model.parsers.MarketParser;
import org.ulpgc.dacd.marketfeeder.model.events.MarketEvent;
import org.ulpgc.dacd.marketfeeder.model.events.MarketEventMapper;
import org.ulpgc.dacd.marketfeeder.model.publisher.EventPublisher;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

    private final MarketFeeder feeder;
    private final MarketParser parser;
    private final MarketEventMapper mapper;
    private final EventPublisher publisher;
    private final List<String> symbols;
    private final int intervalHours;
    private final long pauseMs;

    public MarketController(MarketFeeder feeder, MarketParser parser, MarketEventMapper mapper, EventPublisher publisher, List<String> symbols, int intervalHours, long pauseMs) {
        this.feeder = feeder;
        this.parser = parser;
        this.mapper = mapper;
        this.publisher = publisher;
        this.symbols = symbols;
        this.intervalHours = intervalHours;
        this.pauseMs = pauseMs;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing market scheduler...");
            scheduler.shutdown();
            publisher.close();
        }));

        scheduler.scheduleWithFixedDelay(
                this::runCycleSafely,
                0,
                intervalHours,
                TimeUnit.HOURS
        );

        logger.info("Market controller started. Collection every {} hours for symbols: {}.", intervalHours, symbols);
    }

    private void runCycleSafely() {
        try {
            runCycle();
        } catch (Exception e) {
            logger.error("Unexpected error in market cycle.", e);
        }
    }

    private void runCycle() {
        logger.info("Starting market data collection cycle...");

        for (String symbol : symbols) {
            try {
                processSymbol(symbol);
            } catch (Exception e) {
                logger.error("Error processing symbol {}.", symbol, e);
            }
            pauseBetweenRequests();
        }

        logger.info("Market data collection cycle finished.");
    }

    private void processSymbol(String symbol) {
        logger.info("Querying symbol {}...", symbol);
        String rawResponse = feeder.fetchWeeklySeriesRaw(symbol);
        List<CommoditiesInfo> data = parser.parse(symbol, rawResponse);

        if (data.isEmpty()) {
            logger.warn("No data obtained for {}.", symbol);
            return;
        }

        for (CommoditiesInfo commodity : data) {
            MarketEvent event = mapper.toEvent(commodity);
            publisher.publishEvent("CommodityPrice", event);
        }

        logger.info("Published {} events for {}.", data.size(), symbol);
    }
    private void pauseBetweenRequests() {
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Pause between requests was interrupted.", e);
        }
    }
}
