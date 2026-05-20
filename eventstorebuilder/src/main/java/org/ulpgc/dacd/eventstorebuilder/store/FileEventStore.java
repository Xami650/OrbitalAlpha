package org.ulpgc.dacd.eventstorebuilder.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class FileEventStore implements EventStore {

    private static final Logger logger = LoggerFactory.getLogger(FileEventStore.class);

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final Path rootDirectory;

    public FileEventStore(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public void store(String topic, String jsonEvent) {
        JsonObject event = parseEvent(jsonEvent);
        Path eventFile = buildEventFilePath(topic, event);
        if (isDuplicatedEvent(eventFile, event)) {
            logger.info("Duplicated event ignored in {}", eventFile);
            return;
        }
        appendEvent(eventFile, jsonEvent);

        logger.info("Stored event in {}", eventFile);
    }

    private JsonObject parseEvent(String jsonEvent) {
        return JsonParser.parseString(jsonEvent).getAsJsonObject();
    }

    private Path buildEventFilePath(String topic, JsonObject event) {
        String ss = readRequiredField(event, "ss");
        String ts = readRequiredField(event, "ts");
        String fileDate = formatEventDate(ts);

        return rootDirectory
                .resolve(topic)
                .resolve(ss)
                .resolve(fileDate + ".events");
    }

    private String formatEventDate(String ts) {
        return FILE_DATE_FORMAT.format(Instant.parse(ts));
    }

    private void appendEvent(Path eventFile, String jsonEvent) {
        try {
            Files.createDirectories(eventFile.getParent());
            Files.writeString(
                    eventFile,
                    jsonEvent + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("Error writing event to file " + eventFile, e);
        }
    }

    private String readRequiredField(JsonObject event, String fieldName) {
        if (!event.has(fieldName) || event.get(fieldName).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }

        String value = event.get(fieldName).getAsString();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Blank required field: " + fieldName);
        }
        return value.trim();
    }

    private boolean isDuplicatedEvent(Path eventFile, JsonObject newEvent) {
        if (!Files.exists(eventFile)) {
            return false;
        }

        String newEventKey = getDeduplicationKey(newEvent);

        try (var lines = Files.lines(eventFile)) {
            return lines
                    .map(this::parseEvent)
                    .map(this::getDeduplicationKey)
                    .anyMatch(newEventKey::equals);
        } catch (IOException e) {
            throw new RuntimeException("Error checking duplicated event in file " + eventFile, e);
        }
    }

    private String getDeduplicationKey(JsonObject event) {
        if (event.has("symbol") && event.has("priceTimestamp")) {
            return readRequiredField(event, "symbol") + "|" + readRequiredField(event, "priceTimestamp");
        }

        if (event.has("producerId") && event.has("periodStart")) {
            return readRequiredField(event, "producerId") + "|" + readRequiredField(event, "periodStart");
        }

        return event.toString();
    }
}