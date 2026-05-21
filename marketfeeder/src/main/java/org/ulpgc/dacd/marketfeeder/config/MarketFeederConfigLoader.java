package org.ulpgc.dacd.marketfeeder.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MarketFeederConfigLoader {

    private static final String API_KEY_PROPERTY = "market.api.key";
    private static final String SYMBOLS_PROPERTY = "market.symbols";
    private static final String INTERVAL_HOURS_PROPERTY = "market.interval.hours";
    private static final String MAX_WEEKS_PROPERTY = "market.max.weeks";
    private static final String PAUSE_MS_PROPERTY = "market.pause.ms";
    private static final String BROKER_URL_PROPERTY = "broker.url";
    private static final String TOPIC_PROPERTY = "market.topic";

    private final String configFile;

    public MarketFeederConfigLoader(String configFile) {
        this.configFile = configFile;
    }

    public MarketFeederConfig load() {
        Properties properties = loadProperties();

        return new MarketFeederConfig(
                readApiKey(properties),
                readSymbols(properties),
                readIntervalHours(properties),
                readMaxWeeks(properties),
                readPauseMs(properties),
                readBrokerUrl(properties),
                readTopic(properties)
        );
    }

    private String readApiKey(Properties properties) {
        return readRequiredString(properties, API_KEY_PROPERTY);
    }

    private List<String> readSymbols(Properties properties) {
        return splitCommaSeparatedValues(
                readRequiredString(properties, SYMBOLS_PROPERTY)
        );
    }

    private int readIntervalHours(Properties properties) {
        return readInt(properties, INTERVAL_HOURS_PROPERTY, 24);
    }

    private int readMaxWeeks(Properties properties) {
        return readInt(properties, MAX_WEEKS_PROPERTY, 520);
    }

    private long readPauseMs(Properties properties) {
        return readLong(properties, PAUSE_MS_PROPERTY, 15000L);
    }

    private String readBrokerUrl(Properties properties) {
        return readRequiredString(properties, BROKER_URL_PROPERTY);
    }

    private String readTopic(Properties properties) {
        return readRequiredString(properties, TOPIC_PROPERTY);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = openConfigFile()) {
            properties.load(input);
            return properties;

        } catch (IOException e) {
            throw new RuntimeException("Error reading " + configFile, e);
        }
    }

    private InputStream openConfigFile() {
        InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(configFile);

        if (input == null) {
            throw new IllegalStateException("Configuration file not found: " + configFile);
        }

        return input;
    }

    private String readRequiredString(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }

        return value.trim();
    }

    private int readInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Integer.parseInt(value.trim());
    }

    private long readLong(Properties properties, String key, long defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Long.parseLong(value.trim());
    }

    private List<String> splitCommaSeparatedValues(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}