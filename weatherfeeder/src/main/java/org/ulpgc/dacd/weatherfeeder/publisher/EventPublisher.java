package org.ulpgc.dacd.weatherfeeder.publisher;

public interface EventPublisher {
    void publish(String topicName, String message);
    void close();
}