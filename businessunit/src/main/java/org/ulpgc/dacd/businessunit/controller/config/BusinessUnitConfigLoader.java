package org.ulpgc.dacd.businessunit.controller.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BusinessUnitConfigLoader {

    private static final String BROKER_URL_PROPERTY = "broker.url";
    private static final String CLIENT_ID_PROPERTY = "client.id";
    private static final String TOPICS_PROPERTY = "topics";
    private static final String EVENT_STORE_PATH_PROPERTY = "event.store.path";
    private static final String BATCH_DATAMART_URL_PROPERTY = "batch.datamart.url";
    private static final String SERVING_DATAMART_URL_PROPERTY = "serving.datamart.url";
    private static final String API_PORT_PROPERTY = "api.port";
    private static final String ML_API_URL_PROPERTY = "ml.api.url";
    private static final String DEBOUNCE_DELAY_SECONDS_PROPERTY = "speed.layer.debounce.seconds";

    private final String configFile;

    public BusinessUnitConfigLoader(String configFile) {
        this.configFile = configFile;
    }

    public BusinessUnitConfig load() {
        Properties properties = loadProperties();

        return new BusinessUnitConfig(
                readBrokerUrl(properties),
                readClientId(properties),
                readTopics(properties),
                readEventStorePath(properties),
                readBatchDatamartUrl(properties),
                readServingDatamartUrl(properties),
                readApiPort(properties),
                readMlApiUrl(properties),
                readDebounceDelaySeconds(properties)
        );
    }

    private String readBrokerUrl(Properties properties) {
        return readRequiredString(properties, BROKER_URL_PROPERTY);
    }

    private String readClientId(Properties properties) {
        return readRequiredString(properties, CLIENT_ID_PROPERTY);
    }

    private List<String> readTopics(Properties properties) {
        return splitCommaSeparatedValues(
                readRequiredString(properties, TOPICS_PROPERTY)
        );
    }

    private Path readEventStorePath(Properties properties) {
        return Path.of(readRequiredString(properties, EVENT_STORE_PATH_PROPERTY));
    }

    private String readBatchDatamartUrl(Properties properties) {
        return readRequiredString(properties, BATCH_DATAMART_URL_PROPERTY);
    }

    private String readServingDatamartUrl(Properties properties) {
        return readRequiredString(properties, SERVING_DATAMART_URL_PROPERTY);
    }

    private int readApiPort(Properties properties) {
        return readInt(properties, API_PORT_PROPERTY, 7070);
    }

    private String readMlApiUrl(Properties properties) {
        return readRequiredString(properties, ML_API_URL_PROPERTY);
    }

    private int readDebounceDelaySeconds(Properties properties) {
        return readInt(properties, DEBOUNCE_DELAY_SECONDS_PROPERTY, 5);
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

    private List<String> splitCommaSeparatedValues(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}