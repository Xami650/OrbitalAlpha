package org.ulpgc.dacd.weatherfeeder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.controller.aggregator.WeatherDataAggregator;
import org.ulpgc.dacd.weatherfeeder.controller.chunker.WeeklyDateChunker;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.EventPublisher;
import org.ulpgc.dacd.weatherfeeder.controller.serializer.EventSerializer;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClimateController implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ClimateController.class);

    private final ClimateFeeder feeder;
    private final ProducersInfo producersInfo;
    private final EventPublisher publisher;
    private final WeatherDataAggregator aggregator;
    private final WeeklyDateChunker chunker;
    private final EventSerializer serializer;
    private final WeatherConfig config;
    private final Clock clock;

    private ScheduledExecutorService scheduler;

    public ClimateController(
            ClimateFeeder feeder,
            ProducersInfo producersInfo,
            EventPublisher publisher,
            WeatherDataAggregator aggregator,
            WeeklyDateChunker chunker,
            EventSerializer serializer,
            WeatherConfig config,
            Clock clock
    ) {
        this.feeder = feeder;
        this.producersInfo = producersInfo;
        this.publisher = publisher;
        this.aggregator = aggregator;
        this.chunker = chunker;
        this.serializer = serializer;
        this.config = config;
        this.clock = clock;
    }

    public void start() {
        if (config.mode() == WeatherMode.BACKFILL) {
            runBackfillSafely();
        } else {
            startWeeklyScheduler();
        }
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        publisher.close();
    }

    private void startWeeklyScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        int intervalHours = config.schedule().collectionIntervalHours();
        scheduler.scheduleWithFixedDelay(
                this::runWeeklyCycleSafely,
                0,
                intervalHours,
                TimeUnit.HOURS
        );
        logger.info("Modo WEEKLY iniciado. Recoleccion cada {} horas.", intervalHours);
    }

    private void runBackfillSafely() {
        logger.info("Modo BACKFILL iniciado para {} semanas.", config.backfillWeeks());
        try {
            runBackfill();
            logger.info("Backfill completado correctamente.");
        } catch (Exception e) {
            logger.error("Backfill abortado por error no controlado.", e);
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
        DateRange week = chunker.currentWeek(LocalDate.now(clock));
        forEachProducer(producer -> processBlockSafely(producer, week));
        logger.info("Ciclo WEEKLY finalizado.");
    }

    private void runBackfill() {
        List<DateRange> chunks = chunker.backfillWeeks(LocalDate.now(clock), config.backfillWeeks());
        logger.info("Backfill: {} bloques de {} dias por productor.", chunks.size(), config.schedule().windowDays());

        forEachProducer(producer -> {
            logger.info("Backfill iniciado para {} ({} bloques).", producer.id(), chunks.size());
            for (DateRange chunk : chunks) {
                processBlockSafely(producer, chunk);
            }
            logger.info("Backfill finalizado para {}.", producer.id());
        });
    }

    private void forEachProducer(Consumer<Producer> action) {
        for (String producerId : producersInfo.getAllIds()) {
            Producer producer = producersInfo.getById(producerId);
            if (producer == null) {
                logger.error("Productor desconocido: {}", producerId);
                continue;
            }
            action.accept(producer);
        }
    }

    private void processBlockSafely(Producer producer, DateRange range) {
        try {
            List<WeatherEvent> events = feeder.fetch(producer, range);
            if (events.isEmpty()) {
                logger.warn("Bloque {} de {} sin eventos validos.", range, producer.id());
            } else {
                publishAggregate(events, producer, range);
            }
        } catch (Exception e) {
            logger.error("Bloque {} de {} fallo. Se continua con el siguiente.", range, producer.id(), e);
        } finally {
            pauseBetweenRequests();
        }
    }

    private void publishAggregate(List<WeatherEvent> events, Producer producer, DateRange range) {
        Optional<WeatherAggregate> aggregate = aggregator.aggregate(
                events,
                producer,
                range.startAsApiDate(),
                range.endAsApiDate()
        );

        aggregate.ifPresent(value -> publisher.publish(config.broker().weatherTopic(), serializer.serialize(value)));
    }

    private void pauseBetweenRequests() {
        long pauseMs = config.api().rateLimitPauseMs();
        if (pauseMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("La pausa entre peticiones fue interrumpida.", e);
        }
    }
}