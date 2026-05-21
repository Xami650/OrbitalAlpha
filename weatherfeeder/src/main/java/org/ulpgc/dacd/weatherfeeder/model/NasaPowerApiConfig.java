package org.ulpgc.dacd.weatherfeeder.model;

public record NasaPowerApiConfig(String urlTemplate, long rateLimitPauseMs) {
    public NasaPowerApiConfig {
        if (urlTemplate == null || urlTemplate.isBlank()) {
            throw new IllegalArgumentException("nasa.api.url.template must not be blank.");
        }
        if (rateLimitPauseMs < 0) {
            throw new IllegalArgumentException("nasa.api.rate.limit.pause.ms must not be negative.");
        }
    }
}