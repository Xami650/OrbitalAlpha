package org.ulpgc.dacd.businessunit.controller.batch;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileEventStoreReader implements EventStoreReader {

    private static final String EVENT_FILE_EXTENSION = ".events";

    private final Path eventStorePath;

    public FileEventStoreReader(Path eventStorePath) {
        this.eventStorePath = eventStorePath;
    }

    @Override
    public List<HistoricalEvent> readAllEvents() {
        if (!Files.exists(eventStorePath)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(eventStorePath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isEventFile)
                    .flatMap(this::readEventsFromFile)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Error reading event store from " + eventStorePath, e);
        }
    }

    private boolean isEventFile(Path path) {
        return path.toString().endsWith(EVENT_FILE_EXTENSION);
    }

    private Stream<HistoricalEvent> readEventsFromFile(Path eventFile) {
        try {
            EventFileMetadata metadata = extractMetadata(eventFile);

            return Files.readAllLines(eventFile)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .map(line -> new HistoricalEvent(
                            metadata.topic(),
                            metadata.sourceSystem(),
                            metadata.fileDate(),
                            line
                    ));

        } catch (IOException e) {
            throw new RuntimeException("Error reading event file " + eventFile, e);
        }
    }

    private EventFileMetadata extractMetadata(Path eventFile) {
        Path relativePath = eventStorePath.relativize(eventFile);

        if (relativePath.getNameCount() < 3) {
            throw new IllegalArgumentException("Invalid event store path: " + eventFile);
        }

        String topic = relativePath.getName(0).toString();
        String sourceSystem = relativePath.getName(1).toString();
        String fileName = relativePath.getFileName().toString();
        String fileDate = fileName.replace(EVENT_FILE_EXTENSION, "");

        return new EventFileMetadata(topic, sourceSystem, fileDate);
    }

    private record EventFileMetadata(
            String topic,
            String sourceSystem,
            String fileDate
    ) {
    }
}