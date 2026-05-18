package org.ulpgc.dacd.weatherfeeder.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProducersInfo {

    private final Map<String, Producer> producers;

    public ProducersInfo(Map<String, Producer> producers) {
        if (producers == null) {
            throw new IllegalArgumentException("producers no puede ser null.");
        }
        if (producers.isEmpty()) {
            throw new IllegalArgumentException("producers no puede estar vacio.");
        }
        this.producers = new LinkedHashMap<>(producers);
    }

    public Producer getById(String id) {
        return producers.get(id);
    }

    public List<String> getAllIds() {
        return List.copyOf(producers.keySet());
    }

    public record Producer(
            String id,
            String name,
            String commodityType,
            double latitude,
            double longitude
    ) {
    }
}