package org.ulpgc.dacd.businessunit.controller.processor;

import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;
import java.util.List;
import java.util.Map;

public interface EventProcessor<T> {
    Map<String, T> process(List<HistoricalEvent> historicalEvents);
}