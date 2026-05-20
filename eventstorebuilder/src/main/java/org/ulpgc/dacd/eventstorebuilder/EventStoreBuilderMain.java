package org.ulpgc.dacd.eventstorebuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.eventstorebuilder.config.EventStoreBuilderConfig;
import org.ulpgc.dacd.eventstorebuilder.config.EventStoreBuilderConfigLoader;
import org.ulpgc.dacd.eventstorebuilder.controller.EventStoreController;
import org.ulpgc.dacd.eventstorebuilder.store.FileEventStore;
import org.ulpgc.dacd.eventstorebuilder.subscriber.ActiveMqEventSubscriber;

public class EventStoreBuilderMain {

    private static final Logger logger = LoggerFactory.getLogger(EventStoreBuilderMain.class);
    private static final String CONFIG_FILE = "eventstorebuilder.properties";

    public static void main(String[] args) {
        logger.info("Starting EventStoreBuilder...");
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader(CONFIG_FILE).load();
        logger.info("Configuration loaded. Topics: {}, event store path: {}", config.topics(), config.eventStorePath());

        FileEventStore eventStore = new FileEventStore(config.eventStorePath());

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                config.brokerUrl(),
                config.clientId(),
                config.topics()
        );

        EventStoreController controller = new EventStoreController(subscriber, eventStore);
        controller.start();
    }
}