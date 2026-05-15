package org.ulpgc.dacd.weatherfeeder.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.controller.aggregator.WeatherDataAggregator;
import org.ulpgc.dacd.weatherfeeder.controller.chunker.WeeklyDateChunker;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.ActiveMqEventPublisher;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final ActiveMqEventPublisher publisher;
    private final WeatherDataAggregator aggregator;
    private final WeeklyDateChunker chunker;
    private final WeatherConfig config;
    private final Gson gson;

    public ClimateController(
            ClimateFeeder feeder,
            ProducersInfo producersInfo,
            ActiveMqEventPublisher publisher,
            WeatherConfig config
    ) {
        this.feeder = feeder;
        this.producersInfo = producersInfo;
        this.publisher = publisher;
        this.config = config;
        this.aggregator = new WeatherDataAggregator();
        this.chunker = new WeeklyDateChunker();
        this.gson = createGson();
    }

    public void start() {
        if (config.mode() == WeatherMode.BACKFILL) {
            startBackfill();
        } else {
            startWeekly();
        }
    }

    private void startWeekly() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cerrando scheduler climático...");
            scheduler.shutdown();
            publisher.close();
        }));

        scheduler.scheduleWithFixedDelay(
                this::runWeeklyCycleSafely,
                0,
                COLLECTION_INTERVAL_HOURS,
                TimeUnit.HOURS
        );

        logger.info("Modo WEEKLY iniciado. Recolección cada {} horas.", COLLECTION_INTERVAL_HOURS);
    }

    private void startBackfill() {
        logger.info("Modo BACKFILL iniciado para {} días.", config.backfillDays());
        try {
            runBackfill();
            logger.info("Backfill completado correctamente.");
        } catch (Exception e) {
            logger.error("Backfill abortado por error no controlado.", e);
        } finally {
            publisher.close();
        }
    }

    private void runWeeklyCycleSafely() {
        try {
            runWeeklyCycle();
        } catch (Exception e) {
            logger.error("Error no controlado en el ciclo semanal.", e);
        }
    }

    private void runWeeklyCycle() {
        logger.info("Iniciando ciclo WEEKLY...");
        DateRange week = chunker.currentWeek(LocalDate.now(ZoneOffset.UTC));

        for (String producerId : producersInfo.getAllIds()) {
            Producer producer = producersInfo.getById(producerId);
            if (producer == null) {
                logger.error("Productor desconocido: {}", producerId);
                continue;
            }
            processBlockSafely(producer, week);
        }

        logger.info("Ciclo WEEKLY finalizado.");
    }

    private void runBackfill() {
        List<DateRange> chunks = chunker.backfillWeeks(
                LocalDate.now(ZoneOffset.UTC),
                config.backfillDays()
        );

        logger.info("Backfill: {} bloques de 7 días por productor.", chunks.size());

        for (String producerId : producersInfo.getAllIds()) {
            Producer producer = producersInfo.getById(producerId);
            if (producer == null) {
                logger.error("Productor desconocido: {}", producerId);
                continue;
            }

            logger.info("Backfill iniciado para {} ({} bloques).", producerId, chunks.size());
            for (DateRange chunk : chunks) {
                processBlockSafely(producer, chunk);
            }
            logger.info("Backfill finalizado para {}.", producerId);
        }
    }

    private void processBlockSafely(Producer producer, DateRange range) {
        try {
            List<WeatherEvent> events = feeder.fetch(producer.id(), range);

            if (events.isEmpty()) {
                logger.warn("Bloque {} de {} sin eventos válidos.", range, producer.id());
            } else {
                publishAggregate(events, producer, range);
            }
        } catch (Exception e) {
            logger.error("Bloque {} de {} falló. Se continúa con el siguiente.", range, producer.id(), e);
        } finally {
            pauseBetweenRequests();
        }
    }

    private void publishAggregate(List<WeatherEvent> events, Producer producer, DateRange range) {
        List<WeatherEvent> sorted = events.stream()
                .sorted(Comparator.comparing(WeatherEvent::date))
                .toList();

        Optional<WeatherAggregate> aggregate = aggregator.aggregate(
                sorted,
                producer,
                range.startAsApiDate(),
                range.endAsApiDate()
        );

        if (aggregate.isEmpty()) {
            return;
        }

        publisher.publish(WEATHER_TOPIC, gson.toJson(aggregate.get()));
    }

    private void pauseBetweenRequests() {
        try {
            Thread.sleep(API_RATE_LIMIT_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("La pausa entre peticiones fue interrumpida.", e);
        }
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(
                        Instant.class,
                        (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString())
                )
                .create();
    }
}