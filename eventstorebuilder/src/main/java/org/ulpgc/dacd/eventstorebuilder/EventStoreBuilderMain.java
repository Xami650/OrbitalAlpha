package org.ulpgc.dacd.eventstorebuilder;

import org.ulpgc.dacd.eventstorebuilder.config.EventStoreBuilderConfig;
import org.ulpgc.dacd.eventstorebuilder.config.EventStoreBuilderConfigLoader;
import org.ulpgc.dacd.eventstorebuilder.controller.EventStoreController;
import org.ulpgc.dacd.eventstorebuilder.store.FileEventStore;
import org.ulpgc.dacd.eventstorebuilder.subscriber.ActiveMqEventSubscriber;

public class EventStoreBuilderMain {

    private static final String CONFIG_FILE = "eventstorebuilder.properties";

    public static void main(String[] args) {
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader(CONFIG_FILE).load();

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