package org.ulpgc.weatherfeeder;

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

    private static final List<String> PRODUCERS_TO_TRACK = List.of(
            // WHEAT
            "WHEAT_1", "WHEAT_2", "WHEAT_3", "WHEAT_4", "WHEAT_5",
            // CORN
            "CORN_1", "CORN_2", "CORN_3", "CORN_4", "CORN_5",
            // SOY BEANS
            "SOY_1", "SOY_2", "SOY_3", "SOY_4", "SOY_5",
            // COFFEE
            "COFFEE_1", "COFFEE_2", "COFFEE_3", "COFFEE_4", "COFFEE_5",
            // NATURAL GAS
            "NATGAS_1", "NATGAS_2", "NATGAS_3", "NATGAS_4", "NATGAS_5"
    );

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

        for (String producerId : PRODUCERS_TO_TRACK) {
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
