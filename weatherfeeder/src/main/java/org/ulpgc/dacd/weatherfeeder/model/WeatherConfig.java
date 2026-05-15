package org.ulpgc.dacd.weatherfeeder.model;

import java.nio.file.Path;

public record WeatherConfig(
        WeatherMode mode,
        int backfillDays,
        Path producersFilePath
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
    }
}