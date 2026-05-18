package org.ulpgc.dacd.weatherfeeder.model;

import java.nio.file.Path;

public record WeatherConfig(
        WeatherMode mode,
        int backfillDays,
        Path producersFilePath,
        String sourceSystem,
        BrokerConfig broker,
        NasaPowerApiConfig api,
        ScheduleConfig schedule
) {
    public WeatherConfig {
        if (mode == null) {
            throw new IllegalArgumentException("weather.mode no puede ser null.");
        }
        if (backfillDays <= 0) {
            throw new IllegalArgumentException("weather.backfill.days debe ser > 0.");
        }
        if (producersFilePath == null) {
            throw new IllegalArgumentException("producersFilePath no puede ser null.");
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("weather.source.system no puede estar vacio.");
        }
        if (broker == null) {
            throw new IllegalArgumentException("broker no puede ser null.");
        }
        if (api == null) {
            throw new IllegalArgumentException("api no puede ser null.");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("schedule no puede ser null.");
        }
    }
}