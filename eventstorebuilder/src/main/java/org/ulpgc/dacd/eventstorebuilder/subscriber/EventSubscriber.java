package org.ulpgc.dacd.eventstorebuilder.subscriber;

public interface EventSubscriber {
    void subscribe(EventMessageHandler handler);
    void close();
    interface EventMessageHandler {
        void handle(String topic, String jsonEvent);
    }
}