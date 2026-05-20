package org.ulpgc.dacd.eventstorebuilder.store;

public interface EventStore {
    void store(String topic, String jsonEvent);
}