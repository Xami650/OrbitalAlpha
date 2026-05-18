package org.ulpgc.dacd.weatherfeeder.model;

public record BrokerConfig(String url, String weatherTopic) {
    public BrokerConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("broker.url no puede estar vacio.");
        }
        if (weatherTopic == null || weatherTopic.isBlank()) {
            throw new IllegalArgumentException("broker.topic.weather no puede estar vacio.");
        }
    }
}