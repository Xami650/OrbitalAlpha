package org.ulpgc.dacd.businessunit.controller.speed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.batch.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfig;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeedLayer {

    private static final Logger logger = LoggerFactory.getLogger(SpeedLayer.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final EventSubscriber eventSubscriber;
    private final BatchDatamart batchDatamart;
    private final ServingLayer servingLayer;
    private final BusinessUnitConfig config;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "speed-layer-debounce");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean rebuildInProgress = new AtomicBoolean(false);
    private ScheduledFuture<?> pendingRebuild;

    public SpeedLayer(
            EventSubscriber eventSubscriber,
            BatchDatamart batchDatamart,
            ServingLayer servingLayer,
            BusinessUnitConfig config
    ) {
        this.eventSubscriber = eventSubscriber;
        this.batchDatamart = batchDatamart;
        this.servingLayer = servingLayer;
        this.config = config;
    }

    public void start() {
        eventSubscriber.subscribe(this::processEvent);
        logger.info("Speed layer started");
    }

    private synchronized void processEvent(String topic, String jsonEvent) {
        logger.info("Speed layer received event from topic {}", topic);

        try {
            HistoricalEvent event = toHistoricalEvent(topic, jsonEvent);
            batchDatamart.saveHistoricalEvent(event);
        } catch (Exception e) {
            logger.error("Failed to ingest real-time event into batch datamart", e);
        }

        if (pendingRebuild != null) {
            pendingRebuild.cancel(false);
        }

        pendingRebuild = scheduler.schedule(this::debouncedServingRebuild, config.debounceDelaySeconds(), TimeUnit.SECONDS);
    }

    private HistoricalEvent toHistoricalEvent(String topic, String jsonEvent) {
        JsonObject json = JsonParser.parseString(jsonEvent).getAsJsonObject();
        String ss = json.get("ss").getAsString();
        String ts = json.get("ts").getAsString();
        String fileDate = DATE_FORMAT.format(Instant.parse(ts));
        return new HistoricalEvent(topic, ss, fileDate, jsonEvent);
    }

    // Coalesces rapid event bursts into a single rebuild; AtomicBoolean prevents overlapping rebuilds.
    private void debouncedServingRebuild() {
        if (!rebuildInProgress.compareAndSet(false, true)) {
            logger.info("Serving rebuild already in progress, skipping");
            return;
        }

        try {
            logger.info("Debounced serving layer rebuild triggered");
            servingLayer.rebuild();
        } finally {
            rebuildInProgress.set(false);
        }
    }

    public void stop() {
        eventSubscriber.close();
        scheduler.shutdownNow();
    }
}