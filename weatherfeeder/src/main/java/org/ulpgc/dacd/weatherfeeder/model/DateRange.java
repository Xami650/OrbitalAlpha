package org.ulpgc.dacd.weatherfeeder.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record DateRange(LocalDate start, LocalDate end) {

    private static final DateTimeFormatter API_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public DateRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start y end no pueden ser null.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start no puede ser posterior a end.");
        }
    }

    public String startAsApiDate() {
        return start.format(API_FORMAT);
    }

    public String endAsApiDate() {
        return end.format(API_FORMAT);
    }

    @Override
    public String toString() {
        return startAsApiDate() + "-" + endAsApiDate();
    }
}