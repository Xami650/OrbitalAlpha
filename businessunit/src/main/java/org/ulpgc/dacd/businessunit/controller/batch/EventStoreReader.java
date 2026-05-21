package org.ulpgc.dacd.businessunit.controller.batch;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;

/** Reads historical events from the immutable file-based event store. */
public interface EventStoreReader {
    List<HistoricalEvent> readAllEvents();
}