package org.ulpgc.dacd.weatherfeeder.controller.chunker;

import org.ulpgc.dacd.weatherfeeder.model.DateRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDateChunker {

    private final int windowDays;

    public WeeklyDateChunker(int windowDays) {
        if (windowDays <= 0) {
            throw new IllegalArgumentException("windowDays debe ser > 0.");
        }
        this.windowDays = windowDays;
    }

    public DateRange currentWeek(LocalDate today) {
        requireNonNullToday(today);
        return new DateRange(today.minusDays(windowDays - 1L), today);
    }

    public List<DateRange> backfillWeeks(LocalDate today, int numWeeks) {
        requireNonNullToday(today);
        if (numWeeks < 0) {
            throw new IllegalArgumentException("numWeeks no puede ser negativo.");
        }

        List<DateRange> blocks = new ArrayList<>(numWeeks);

        for (int i = 0; i < numWeeks; i++) {
            LocalDate end = today.minusDays((long) windowDays * i);
            LocalDate start = end.minusDays(windowDays - 1L);
            blocks.add(new DateRange(start, end));
        }

        return blocks;
    }

    private static void requireNonNullToday(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("today no puede ser null.");
        }
    }
}