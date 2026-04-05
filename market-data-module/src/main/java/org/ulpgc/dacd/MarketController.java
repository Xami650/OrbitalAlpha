package org.ulpgc.dacd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

    private static final int COLLECTION_INTERVAL_HOURS = 24;
    private static final long API_RATE_LIMIT_PAUSE_MS = 15000;

    private static final List<String> SYMBOLS_TO_TRACK = List.of("WEAT", "CORN", "SOYB", "JO", "UNG");

    private final MarketFeeder feeder;
    private final MarketStore store;

    public MarketController(MarketFeeder feeder, MarketStore store) {
        this.feeder = feeder;
        this.store = store;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando scheduler de mercado...");
            scheduler.shutdown();
        }));

        scheduler.scheduleWithFixedDelay(
                this::runCycleSafely,
                0,
                COLLECTION_INTERVAL_HOURS,
                TimeUnit.HOURS
        );

        logger.info("Controlador iniciado. Recolección cada {} horas.", COLLECTION_INTERVAL_HOURS);
    }

    private void runCycleSafely() {
        try {
            runCycle();
        } catch (Exception e) {
            logger.error("Error no controlado en el ciclo de mercado.", e);
        }
    }

    private void runCycle() {
        logger.info("Iniciando ciclo de recolección de mercado...");

        for (String symbol : SYMBOLS_TO_TRACK) {
            logger.info("Consultando símbolo {}...", symbol);

            List<MarketData> marketData = feeder.fetch(symbol);

            if (marketData.isEmpty()) {
                logger.warn("No se obtuvieron datos para {}.", symbol);
            } else {
                store.store(marketData);
            }

            pauseBetweenRequests();
        }

        logger.info("Ciclo de mercado finalizado.");
    }

    private void pauseBetweenRequests() {
        try {
            Thread.sleep(API_RATE_LIMIT_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("La pausa entre peticiones fue interrumpida.", e);
        }
    }
}