package org.ulpgc.dacd.weatherfeeder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ClimateData;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.feeders.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.model.storers.ClimateStore;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClimateController {

    private static final Logger logger = LoggerFactory.getLogger(ClimateController.class);

    private static final int COLLECTION_INTERVAL_HOURS = 24;
    private static final long API_RATE_LIMIT_PAUSE_MS = 1000;

    private final ClimateFeeder feeder;
    private final ClimateStore store;
    private final ProducersInfo producersInfo;

    public ClimateController(ClimateFeeder feeder, ClimateStore store, ProducersInfo producersInfo) {
        this.feeder = feeder;
        this.store = store;
        this.producersInfo = producersInfo;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando scheduler climático...");
            scheduler.shutdown();
        }));

        scheduler.scheduleWithFixedDelay(
                this::runCycleSafely,
                0,
                COLLECTION_INTERVAL_HOURS,
                TimeUnit.HOURS
        );

        logger.info("Controlador climático iniciado. Recolección cada {} horas.", COLLECTION_INTERVAL_HOURS);
    }

    private void runCycleSafely() {
        try {
            runCycle();
        } catch (Exception e) {
            logger.error("Error no controlado en el ciclo climático.", e);
        }
    }

    private void runCycle() {
        logger.info("Iniciando ciclo de recolección climática...");

        for (String producerId : producersInfo.getAllIds()) {
            logger.info("Consultando productor o región {}...", producerId);

            List<ClimateData> climateData = feeder.fetch(producerId);

            if (climateData.isEmpty()) {
                logger.warn("No se obtuvieron datos para {}.", producerId);
            } else {
                store.store(climateData);
            }

            pauseBetweenRequests();
        }

        logger.info("Ciclo climático finalizado.");
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