package org.ulpgc.dacd.businessunit.controller.speed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.batch.BatchLayer;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;

public class SpeedLayer {

    private static final Logger logger = LoggerFactory.getLogger(SpeedLayer.class);

    private final EventSubscriber eventSubscriber;
    private final BatchLayer batchLayer;
    private final ServingLayer servingLayer;

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

    private void processEvent(String topic, String jsonEvent) {
        logger.info("Speed layer received event from topic {}", topic);

        batchLayer.rebuild();
        servingLayer.rebuild();
    }

    public void stop() {
        eventSubscriber.close();
    }
}