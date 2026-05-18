package org.ulpgc.dacd.businessunit.controller.speed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.batch.BatchLayer;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeedLayer {

    private static final Logger logger = LoggerFactory.getLogger(SpeedLayer.class);
    private static final long DEBOUNCE_DELAY_SECONDS = 5;

    private final EventSubscriber eventSubscriber;
    private final BatchLayer batchLayer;
    private final ServingLayer servingLayer;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean rebuildInProgress = new AtomicBoolean(false);
    private ScheduledFuture<?> pendingRebuild;

    public SpeedLayer(
            EventSubscriber eventSubscriber,
            BatchLayer batchLayer,
            ServingLayer servingLayer
    ) {
        this.eventSubscriber = eventSubscriber;
        this.batchLayer = batchLayer;
        this.servingLayer = servingLayer;
    }

    public void start() {
        eventSubscriber.subscribe(this::processEvent);
        logger.info("Speed layer started");
    }

    private synchronized void processEvent(String topic, String jsonEvent) {
        logger.info("Speed layer received event from topic {}", topic);

        if (pendingRebuild != null) {
            pendingRebuild.cancel(false);
        }

        pendingRebuild = scheduler.schedule(this::debouncedRebuild, DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void debouncedRebuild() {
        if (!rebuildInProgress.compareAndSet(false, true)) {
            logger.info("Rebuild already in progress, skipping");
            return;
        }

        try {
            logger.info("Debounced rebuild triggered");
            batchLayer.rebuild();
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