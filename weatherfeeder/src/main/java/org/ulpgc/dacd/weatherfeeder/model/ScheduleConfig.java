package org.ulpgc.dacd.weatherfeeder.model;

public record ScheduleConfig(int collectionIntervalHours, int windowDays, int expectedDays) {
    public ScheduleConfig {
        if (collectionIntervalHours <= 0) {
            throw new IllegalArgumentException("schedule.collection.interval.hours debe ser > 0.");
        }
        if (windowDays <= 0) {
            throw new IllegalArgumentException("schedule.window.days debe ser > 0.");
        }
        if (expectedDays <= 0) {
            throw new IllegalArgumentException("schedule.expected.days debe ser > 0.");
        }
    }
}