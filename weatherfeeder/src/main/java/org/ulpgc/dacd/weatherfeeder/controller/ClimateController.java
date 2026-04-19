package org.ulpgc.dacd.weatherfeeder.controller;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.events.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.mappers.WeatherEventMapper;
import org.ulpgc.dacd.weatherfeeder.model.ClimateData;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.feeders.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.publisher.EventPublisher;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClimateController {

    private static final Logger logger = LoggerFactory.getLogger(ClimateController.class);

    private static final int COLLECTION_INTERVAL_HOURS = 24;
    private static final long API_RATE_LIMIT_PAUSE_MS = 1000;
    private static final String WEATHER_TOPIC = "Weather";

    private final ClimateFeeder feeder;
    private final ProducersInfo producersInfo;
    private final EventPublisher publisher;
    private final WeatherEventMapper eventMapper;
    private final Gson gson;

    public ClimateController(ClimateFeeder feeder, ProducersInfo producersInfo, EventPublisher publisher) {
        this.feeder = feeder;
        this.producersInfo = producersInfo;
        this.publisher = publisher;
        this.eventMapper = new WeatherEventMapper();
        this.gson = new Gson();
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando scheduler climático...");
            scheduler.shutdown();
            publisher.close();
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

            List<ClimateData> climateDataList = feeder.fetch(producerId);

            if (climateDataList.isEmpty()) {
                logger.warn("No se obtuvieron datos para {}.", producerId);
            } else {
                publishEvents(climateDataList);
            }

            pauseBetweenRequests();
        }

        logger.info("Ciclo climático finalizado.");
    }

    private void publishEvents(List<ClimateData> climateDataList) {
        for (ClimateData data : climateDataList) {
            WeatherEvent event = eventMapper.map(data);
            String json = gson.toJson(event);
            publisher.publish(WEATHER_TOPIC, json);
        }
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