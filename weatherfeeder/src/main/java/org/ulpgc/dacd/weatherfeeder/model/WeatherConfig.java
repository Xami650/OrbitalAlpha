package org.ulpgc.dacd.weatherfeeder.model;

import java.nio.file.Path;

public record WeatherConfig(
        WeatherMode mode,
        int backfillWeeks,
        Path producersFilePath,
        String sourceSystem,
        BrokerConfig broker,
        NasaPowerApiConfig api,
        ScheduleConfig schedule
) {
    public WeatherConfig {
        if (mode == null) {
            throw new IllegalArgumentException("weather.mode must not be null.");
        }
        if (backfillWeeks <= 0) {
            throw new IllegalArgumentException("weather.backfill.weeks must be > 0.");
        }
        if (producersFilePath == null) {
            throw new IllegalArgumentException("producersFilePath must not be null.");
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("weather.source.system must not be blank.");
        }
        if (broker == null) {
            throw new IllegalArgumentException("broker must not be null.");
        }
        if (api == null) {
            throw new IllegalArgumentException("api must not be null.");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("schedule must not be null.");
        }
    }
}