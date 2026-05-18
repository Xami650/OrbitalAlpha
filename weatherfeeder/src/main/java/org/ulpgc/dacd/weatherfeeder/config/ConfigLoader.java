package org.ulpgc.dacd.weatherfeeder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.BrokerConfig;
import org.ulpgc.dacd.weatherfeeder.model.NasaPowerApiConfig;
import org.ulpgc.dacd.weatherfeeder.model.ScheduleConfig;
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

    private static final String KEY_MODE = "weather.mode";
    private static final String KEY_BACKFILL_DAYS = "weather.backfill.days";
    private static final String KEY_PRODUCERS_FILE = "weather.producers.file";
    private static final String KEY_SOURCE_SYSTEM = "weather.source.system";
    private static final String KEY_BROKER_URL = "broker.url";
    private static final String KEY_BROKER_TOPIC_WEATHER = "broker.topic.weather";
    private static final String KEY_NASA_URL_TEMPLATE = "nasa.api.url.template";
    private static final String KEY_NASA_RATE_LIMIT_PAUSE_MS = "nasa.api.rate.limit.pause.ms";
    private static final String KEY_COLLECTION_INTERVAL_HOURS = "schedule.collection.interval.hours";
    private static final String KEY_WINDOW_DAYS = "schedule.window.days";
    private static final String KEY_EXPECTED_DAYS = "schedule.expected.days";

    private final Properties properties;

    public ConfigLoader() {
        this(loadFromClasspathOrCwd(DEFAULT_PROPERTIES_FILE));
    }


    public ConfigLoader(Properties properties) {
        this.properties = properties != null ? properties : new Properties();
    }

    public WeatherConfig load() {
        WeatherConfig config = new WeatherConfig(
                parseMode(),
                parsePositiveInt(KEY_BACKFILL_DAYS),
                Paths.get(requireString(KEY_PRODUCERS_FILE)),
                requireString(KEY_SOURCE_SYSTEM),
                new BrokerConfig(requireString(KEY_BROKER_URL), requireString(KEY_BROKER_TOPIC_WEATHER)),
                new NasaPowerApiConfig(requireString(KEY_NASA_URL_TEMPLATE), parseNonNegativeLong(KEY_NASA_RATE_LIMIT_PAUSE_MS)),
                new ScheduleConfig(
                        parsePositiveInt(KEY_COLLECTION_INTERVAL_HOURS),
                        parsePositiveInt(KEY_WINDOW_DAYS),
                        parsePositiveInt(KEY_EXPECTED_DAYS)
                )
        );

        logger.info("Configuracion cargada: mode={}, backfillDays={}, producersFile={}",
                config.mode(), config.backfillDays(), config.producersFilePath());

        return config;
    }

    private WeatherMode parseMode() {
        String raw = requireString(KEY_MODE);
        try {
            return WeatherMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Valor invalido para " + KEY_MODE + ": '" + raw + "'. Valores validos: WEEKLY, BACKFILL.", e);
        }
    }

    private int parsePositiveInt(String key) {
        String raw = requireString(key);
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(key + " debe ser > 0: " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " no es un entero valido: " + raw, e);
        }
    }

    private long parseNonNegativeLong(String key) {
        String raw = requireString(key);
        try {
            long value = Long.parseLong(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException(key + " no puede ser negativo: " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " no es un entero valido: " + raw, e);
        }
    }

    private String requireString(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad obligatoria '" + key + "' en " + DEFAULT_PROPERTIES_FILE);
        }
        return value.trim();
    }

    private static Properties loadFromClasspathOrCwd(String resource) {
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
                return props;
            } catch (IOException e) {
                logger.warn("No se pudo leer {} del cwd.", cwdFile, e);
            }
        }

        throw new IllegalStateException("No se encontro " + resource + " ni en el classpath ni en el cwd.");
    }
}