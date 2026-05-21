package org.ulpgc.dacd.eventstorebuilder.store;

/** Persists incoming events into the immutable, file-based event store. */
public interface EventStore {
    void store(String topic, String jsonEvent);
}