package org.ulpgc.dacd.eventstorebuilder;

import org.ulpgc.dacd.eventstorebuilder.controller.EventStoreController;
import org.ulpgc.dacd.eventstorebuilder.store.FileEventStore;
import org.ulpgc.dacd.eventstorebuilder.subscriber.ActiveMqEventSubscriber;

import java.nio.file.Path;
import java.util.List;

public class EventStoreBuilderMain {

    public static void main(String[] args) {
        String brokerUrl = System.getenv("BROKER_URL");
        String clientId = System.getenv("CLIENT_ID");
        String eventStorePath = System.getenv("EVENT_STORE_PATH");
        String topicsEnv = System.getenv("TOPICS");

        if (brokerUrl == null || clientId == null || eventStorePath == null || topicsEnv == null) {
            System.err.println("Error: Faltan variables de entorno. Revisa tu configuración o archivo .env");
            System.exit(1);
        }

        List<String> topics = List.of(topicsEnv.split(","));

        FileEventStore eventStore = new FileEventStore(Path.of(eventStorePath));

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                brokerUrl,
                clientId,
                topics
        );

        EventStoreController controller = new EventStoreController(subscriber, eventStore);
        controller.start();
    }
}