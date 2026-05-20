package org.ulpgc.dacd.businessunit.controller.batch;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;

public interface EventStoreReader {
    List<HistoricalEvent> readAllEvents();
}