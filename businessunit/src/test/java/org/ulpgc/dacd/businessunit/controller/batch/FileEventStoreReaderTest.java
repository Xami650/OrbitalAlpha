package org.ulpgc.dacd.businessunit.controller.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileEventStoreReaderTest {

    @TempDir
    Path eventStore;

    @Test
    void readsEventsFromValidEventStoreLayout() throws IOException {
        Path file = eventStore.resolve("CommodityPrice/AlphaVantage/20260501.events");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {"symbol":"WEAT","close":24.61}
                {"symbol":"CORN","close":18.50}
                """);

        List<HistoricalEvent> events = new FileEventStoreReader(eventStore).readAllEvents();

        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.topic().equals("CommodityPrice"));
        assertThat(events).allMatch(e -> e.sourceSystem().equals("AlphaVantage"));
        assertThat(events).allMatch(e -> e.fileDate().equals("20260501"));
    }

    @Test
    void skipsBlankLines() throws IOException {
        Path file = eventStore.resolve("Weather/weatherfeeder/20260501.events");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {"commodityType":"WHEAT"}

                {"commodityType":"CORN"}

                """);

        List<HistoricalEvent> events = new FileEventStoreReader(eventStore).readAllEvents();

        assertThat(events).hasSize(2);
    }

    @Test
    void returnsEmptyListWhenEventStoreDoesNotExist() {
        Path missing = eventStore.resolve("nonexistent");

        List<HistoricalEvent> events = new FileEventStoreReader(missing).readAllEvents();

        assertThat(events).isEmpty();
    }

    @Test
    void readsEventsFromMultipleFiles() throws IOException {
        Path file1 = eventStore.resolve("CommodityPrice/AlphaVantage/20260501.events");
        Path file2 = eventStore.resolve("CommodityPrice/AlphaVantage/20260502.events");
        Files.createDirectories(file1.getParent());
        Files.writeString(file1, "{\"symbol\":\"WEAT\"}\n");
        Files.writeString(file2, "{\"symbol\":\"CORN\"}\n");

        List<HistoricalEvent> events = new FileEventStoreReader(eventStore).readAllEvents();

        assertThat(events).hasSize(2);
    }
}
