package org.ulpgc.dacd.businessunit.controller.speed;

public interface EventSubscriber extends AutoCloseable {
    void subscribe(EventMessageHandler handler);
    
    @Override
    void close();

    @FunctionalInterface
    interface EventMessageHandler {
        void handle(String topic, String jsonEvent);
    }
}