package org.ulpgc.dacd.businessunit.controller.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;

public class BatchLayer {

    private static final Logger logger = LoggerFactory.getLogger(BatchLayer.class);

    private final EventStoreReader eventStoreReader;
    private final BatchDatamart batchDatamart;

    public BatchLayer(EventStoreReader eventStoreReader, BatchDatamart batchDatamart) {
        this.eventStoreReader = eventStoreReader;
        this.batchDatamart = batchDatamart;
    }

    public void rebuild() {
        logger.info("Starting batch layer rebuild...");

        List<HistoricalEvent> historicalEvents = eventStoreReader.readAllEvents();

        batchDatamart.clear();
        batchDatamart.saveHistoricalEvents(historicalEvents);

        logger.info("Batch layer rebuild finished. Loaded {} historical events.", historicalEvents.size());
    }
}