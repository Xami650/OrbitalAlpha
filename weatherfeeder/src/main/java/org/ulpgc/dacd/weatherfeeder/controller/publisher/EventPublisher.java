package org.ulpgc.dacd.weatherfeeder.controller.publisher;

public interface EventPublisher extends AutoCloseable {
    void publish(String topicName, String message);

    @Override
    void close();
}