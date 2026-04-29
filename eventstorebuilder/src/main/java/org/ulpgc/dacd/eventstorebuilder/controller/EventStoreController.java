package org.ulpgc.dacd.eventstorebuilder.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.eventstorebuilder.store.EventStore;
import org.ulpgc.dacd.eventstorebuilder.subscriber.EventSubscriber;

public class EventStoreController {

    private static final Logger logger = LoggerFactory.getLogger(EventStoreController.class);

    private final EventSubscriber subscriber;
    private final EventStore eventStore;

    public EventStoreController(EventSubscriber subscriber, EventStore eventStore) {
        this.subscriber = subscriber;
        this.eventStore = eventStore;
    }

    public void start(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing Event Store Builder...");
            subscriber.close();
        }));

        subscriber.subscribe((topic, jsonEvent) -> {
            try {
                eventStore.store(topic, jsonEvent);
            } catch (Exception e) {
                logger.error("Error storing event from topic {}", topic, e);
            }
        });

        logger.info("Event Store Builder started.");
    }
}