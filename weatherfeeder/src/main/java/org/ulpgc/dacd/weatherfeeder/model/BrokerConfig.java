package org.ulpgc.dacd.weatherfeeder.model;

public record BrokerConfig(String url, String weatherTopic) {
    public BrokerConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("broker.url must not be blank.");
        }
        if (weatherTopic == null || weatherTopic.isBlank()) {
            throw new IllegalArgumentException("broker.topic.weather must not be blank.");
        }
    }
}