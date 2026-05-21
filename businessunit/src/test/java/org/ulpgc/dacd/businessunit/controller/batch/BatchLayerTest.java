package org.ulpgc.dacd.businessunit.controller.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchLayerTest {

    private InMemoryEventStoreReader reader;
    private InMemoryBatchDatamart datamart;
    private BatchLayer batchLayer;

    @BeforeEach
    void setUp() {
        reader = new InMemoryEventStoreReader();
        datamart = new InMemoryBatchDatamart();
        batchLayer = new BatchLayer(reader, datamart);
    }

    @Test
    void rebuildLoadsEventsFromReaderIntoDatamart() {
        reader.add(new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260520", "{\"symbol\":\"WEAT\"}"));
        reader.add(new HistoricalEvent("Weather", "weatherfeeder", "20260520", "{\"commodityType\":\"WHEAT\"}"));

        batchLayer.rebuild();

        assertThat(datamart.findAllHistoricalEvents()).hasSize(2);
    }

    @Test
    void rebuildClearsDatamartBeforeLoading() {
        reader.add(new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260520", "{\"symbol\":\"WEAT\"}"));
        batchLayer.rebuild();
        assertThat(datamart.findAllHistoricalEvents()).hasSize(1);

        batchLayer.rebuild();
        assertThat(datamart.findAllHistoricalEvents()).hasSize(1);
    }

    @Test
    void rebuildWithNoEventsResultsInEmptyDatamart() {
        batchLayer.rebuild();

        assertThat(datamart.findAllHistoricalEvents()).isEmpty();
    }

    @Test
    void rebuildPreservesEventFields() {
        HistoricalEvent event = new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260520", "{\"symbol\":\"CORN\",\"close\":4.5}");
        reader.add(event);

        batchLayer.rebuild();

        HistoricalEvent stored = datamart.findAllHistoricalEvents().getFirst();
        assertThat(stored.topic()).isEqualTo("CommodityPrice");
        assertThat(stored.sourceSystem()).isEqualTo("AlphaVantage");
        assertThat(stored.fileDate()).isEqualTo("20260520");
        assertThat(stored.rawJson()).contains("CORN");
    }

    private static class InMemoryEventStoreReader implements EventStoreReader {
        private final List<HistoricalEvent> events = new ArrayList<>();

        void add(HistoricalEvent event) { events.add(event); }

        @Override
        public List<HistoricalEvent> readAllEvents() { return List.copyOf(events); }
    }

    private static class InMemoryBatchDatamart implements BatchDatamart {
        private final List<HistoricalEvent> events = new ArrayList<>();

        @Override public void initialize() { events.clear(); }
        @Override public void clear() { events.clear(); }
        @Override public void saveHistoricalEvent(HistoricalEvent event) { events.add(event); }
        @Override public void saveHistoricalEvents(List<HistoricalEvent> events) { this.events.addAll(events); }
        @Override public List<HistoricalEvent> findAllHistoricalEvents() { return List.copyOf(events); }
    }
}
