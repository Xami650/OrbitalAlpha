package org.ulpgc.dacd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClimateController {

    private static final Logger logger = LoggerFactory.getLogger(ClimateController.class);

    private static final int COLLECTION_INTERVAL_HOURS = 24;
    private static final long API_RATE_LIMIT_PAUSE_MS = 1000;

    private static final List<String> LOCATIONS_TO_TRACK = List.of("LPA", "MAD", "BCN", "SVQ", "VLC");

    private final ClimateFeeder feeder;
    private final ClimateStore store;

    public ClimateController(ClimateFeeder feeder, ClimateStore store) {
        this.feeder = feeder;
        this.store = store;
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

        for (String locationId : LOCATIONS_TO_TRACK) {
            logger.info("Consultando ubicación {}...", locationId);

            List<ClimateData> climateData = feeder.fetch(locationId);

            if (climateData.isEmpty()) {
                logger.warn("No se obtuvieron datos para {}.", locationId);
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
