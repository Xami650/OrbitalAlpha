package org.ulpgc.dacd.businessunit.controller.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteBatchDatamartTest {

    @TempDir
    Path tempDir;

    private SqliteBatchDatamart datamart;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("batch_test.db");
        datamart = new SqliteBatchDatamart(url);
        datamart.initialize();
    }

    @Test
    void savesAndReadsBackSingleEvent() {
        HistoricalEvent event = new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260501", "{\"symbol\":\"WEAT\"}");

        datamart.saveHistoricalEvent(event);
        List<HistoricalEvent> result = datamart.findAllHistoricalEvents();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().topic()).isEqualTo("CommodityPrice");
        assertThat(result.getFirst().sourceSystem()).isEqualTo("AlphaVantage");
        assertThat(result.getFirst().fileDate()).isEqualTo("20260501");
        assertThat(result.getFirst().rawJson()).isEqualTo("{\"symbol\":\"WEAT\"}");
    }

    @Test
    void savesMultipleEventsInBatch() {
        List<HistoricalEvent> events = List.of(
                new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260501", "{\"symbol\":\"WEAT\"}"),
                new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260501", "{\"symbol\":\"CORN\"}"),
                new HistoricalEvent("Weather", "weatherfeeder", "20260501", "{\"commodityType\":\"WHEAT\"}")
        );

        datamart.saveHistoricalEvents(events);

        assertThat(datamart.findAllHistoricalEvents()).hasSize(3);
    }

    @Test
    void clearRemovesAllEvents() {
        datamart.saveHistoricalEvent(new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260501", "{}"));

        datamart.clear();

        assertThat(datamart.findAllHistoricalEvents()).isEmpty();
    }

    @Test
    void saveHistoricalEventsOnEmptyListDoesNothing() {
        datamart.saveHistoricalEvents(List.of());

        assertThat(datamart.findAllHistoricalEvents()).isEmpty();
    }
}
