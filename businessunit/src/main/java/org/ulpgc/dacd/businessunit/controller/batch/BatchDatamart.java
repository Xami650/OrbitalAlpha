package org.ulpgc.dacd.businessunit.controller.batch;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;

/** Stores historical events reconstructed from the file event store (batch view of the Lambda Architecture). */
public interface BatchDatamart {
    void initialize();
    void clear();
    void saveHistoricalEvent(HistoricalEvent event);
    void saveHistoricalEvents(List<HistoricalEvent> events);
    List<HistoricalEvent> findAllHistoricalEvents();
}
