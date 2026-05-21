package org.ulpgc.dacd.marketfeeder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.controller.feeder.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.publisher.MarketPublisher;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketController implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

    private final MarketFeeder feeder;
    private final MarketPublisher publisher;
    private final List<String> symbols;
    private final int intervalHours;
    private final long pauseMs;

    private ScheduledExecutorService scheduler;

    public MarketController(MarketFeeder feeder, MarketPublisher publisher, List<String> symbols, int intervalHours, long pauseMs) {
        this.feeder = feeder;
        this.publisher = publisher;
        this.symbols = symbols;
        this.intervalHours = intervalHours;
        this.pauseMs = pauseMs;
    }

    public void start() {
        scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing market scheduler...");
            this.close();
        }));
        scheduler.scheduleWithFixedDelay(
                this::runCycleSafely,
                0,
                intervalHours,
                TimeUnit.HOURS
        );

        logger.info("Market controller started. Collection every {} hours for symbols: {}.", intervalHours, symbols);
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        publisher.close();
    }

    private void runCycleSafely(){
        try {
            runCycle();
        } catch (RuntimeException e) {
            logger.error("Error 500. Aborting cycle.", e);
        } catch (Exception e) {
            logger.error("Unexpected error.", e);
        }
    }

    private void runCycle() {
        logger.info("Starting market data collection cycle...");
        symbols.forEach(this::processSymbolWithApiPause);
        logger.info("Market data collection cycle finished.");
    }

    private void processSymbolWithApiPause(String symbol) {
        processSymbolSafely(symbol);
        pauseBetweenRequests();
    }

    private void processSymbolSafely(String symbol) {
        try {
            processSymbol(symbol);
        } catch (com.google.gson.JsonSyntaxException e) {
            logger.error("Error parsing JSON for {}. Next symbol...", symbol, e);
        }
    }

    private void processSymbol(String symbol) {
        logger.info("Querying symbol {}...", symbol);
        List<MarketEvent> events = feeder.getMarketData(symbol);

        if (events.isEmpty()) {
            logger.warn("No data obtained for {}.", symbol);
            return;
        }
        events.forEach(publisher::publish);
        logger.info("Published {} events for {}.", events.size(), symbol);
    }

    private void pauseBetweenRequests() {
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}