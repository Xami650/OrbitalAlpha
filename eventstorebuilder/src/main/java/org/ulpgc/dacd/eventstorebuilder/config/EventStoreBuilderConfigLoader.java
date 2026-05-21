package org.ulpgc.dacd.eventstorebuilder.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EventStoreBuilderConfigLoader {

    private static final String BROKER_URL_PROPERTY = "broker.url";
    private static final String CLIENT_ID_PROPERTY = "client.id";
    private static final String EVENT_STORE_PATH_PROPERTY = "event.store.path";
    private static final String TOPICS_PROPERTY = "topics";

    private final String configFile;

    public EventStoreBuilderConfigLoader(String configFile) {
        this.configFile = configFile;
    }

    public EventStoreBuilderConfig load() {
        Properties properties = loadProperties();

        return new EventStoreBuilderConfig(
                readBrokerUrl(properties),
                readClientId(properties),
                readEventStorePath(properties),
                readTopics(properties)
        );
    }

    private String readBrokerUrl(Properties properties) {
        return readRequiredString(properties, BROKER_URL_PROPERTY);
    }

    private String readClientId(Properties properties) {
        return readRequiredString(properties, CLIENT_ID_PROPERTY);
    }

    private Path readEventStorePath(Properties properties) {
        return Path.of(readRequiredString(properties, EVENT_STORE_PATH_PROPERTY));
    }

    private List<String> readTopics(Properties properties) {
        return splitCommaSeparatedValues(
                readRequiredString(properties, TOPICS_PROPERTY)
        );
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

    private List<String> splitCommaSeparatedValues(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}