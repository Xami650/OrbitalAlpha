package org.ulpgc.dacd.weatherfeeder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final Path DEFAULT_PRODUCERS_FILE = Paths.get("config", "producers.csv");

    private static final String KEY_MODE = "weather.mode";
    private static final String KEY_BACKFILL_DAYS = "weather.backfill.days";
    private static final String KEY_PRODUCERS_FILE = "weather.producers.file";

    private static final String ENV_MODE = "WEATHER_MODE";
    private static final String ENV_BACKFILL_DAYS = "WEATHER_BACKFILL_DAYS";
    private static final String ENV_PRODUCERS_FILE = "WEATHER_PRODUCERS_FILE";

    private static final WeatherMode DEFAULT_MODE = WeatherMode.WEEKLY;
    private static final int DEFAULT_BACKFILL_DAYS = 520;

    private final Properties fileProperties;
    private final EnvAccessor env;

    public ConfigLoader() {
        this(loadFromClasspath(DEFAULT_PROPERTIES_FILE), System::getenv);
    }

    public ConfigLoader(Properties fileProperties, EnvAccessor env) {
        this.fileProperties = fileProperties != null ? fileProperties : new Properties();
        this.env = env != null ? env : name -> null;
    }

    public WeatherConfig load() {
        WeatherMode mode = resolveMode();
        int backfillDays = resolveBackfillDays();
        Path producersFile = resolveProducersFile();

        logger.info("Configuración cargada: mode={}, backfillDays={}, producersFile={}",
                mode, backfillDays, producersFile);

        return new WeatherConfig(mode, backfillDays, producersFile);
    }

    private WeatherMode resolveMode() {
        String raw = resolve(ENV_MODE, KEY_MODE);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MODE;
        }

        try {
            return WeatherMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Valor inválido para weather.mode: '" + raw + "'. Valores válidos: WEEKLY, BACKFILL.", e);
        }
    }

    private int resolveBackfillDays() {
        String raw = resolve(ENV_BACKFILL_DAYS, KEY_BACKFILL_DAYS);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_BACKFILL_DAYS;
        }

        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("weather.backfill.days debe ser > 0: " + raw);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("weather.backfill.days no es un entero válido: " + raw, e);
        }
    }

    private Path resolveProducersFile() {
        String raw = resolve(ENV_PRODUCERS_FILE, KEY_PRODUCERS_FILE);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PRODUCERS_FILE;
        }
        return Paths.get(raw.trim());
    }

    private String resolve(String envKey, String propKey) {
        String envValue = env.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return fileProperties.getProperty(propKey);
    }

    private static Properties loadFromClasspath(String resource) {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (IOException e) {
            logger.warn("No se pudo leer {} del classpath.", resource, e);
        }

        Path cwdFile = Paths.get(resource);
        if (Files.exists(cwdFile)) {
            try (InputStream in = Files.newInputStream(cwdFile)) {
                props.load(in);
            } catch (IOException e) {
                logger.warn("No se pudo leer {} del cwd.", cwdFile, e);
            }
        }
        return props;
    }

    @FunctionalInterface
    public interface EnvAccessor {
        String get(String name);
    }
}