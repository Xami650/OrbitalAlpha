package org.ulpgc.dacd.businessunit.controller.processor;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;
import java.util.List;
import java.util.Map;

/** Transforms raw historical events into aggregated metrics for a specific data domain. */
public interface EventProcessor<T> {
    Map<String, T> process(List<HistoricalEvent> historicalEvents);
}