package org.ulpgc.dacd.weatherfeeder.model;

public record ScheduleConfig(int collectionIntervalHours, int windowDays, int expectedDays) {
    public ScheduleConfig {
        if (collectionIntervalHours <= 0) {
            throw new IllegalArgumentException("schedule.collection.interval.hours must be > 0.");
        }
        if (windowDays <= 0) {
            throw new IllegalArgumentException("schedule.window.days must be > 0.");
        }
        if (expectedDays <= 0) {
            throw new IllegalArgumentException("schedule.expected.days must be > 0.");
        }
    }
}