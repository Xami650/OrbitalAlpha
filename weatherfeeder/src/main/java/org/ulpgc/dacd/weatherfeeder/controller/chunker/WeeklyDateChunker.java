package org.ulpgc.dacd.weatherfeeder.controller.chunker;

import org.ulpgc.dacd.weatherfeeder.model.DateRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeeklyDateChunker {

    private static final int WINDOW_DAYS = 7;

    public DateRange currentWeek(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("today no puede ser null.");
        }
        return new DateRange(today.minusDays(WINDOW_DAYS - 1L), today);
    }

    public List<DateRange> backfillWeeks(LocalDate today, int backfillDays) {
        if (today == null) {
            throw new IllegalArgumentException("today no puede ser null.");
        }
        if (backfillDays < 0) {
            throw new IllegalArgumentException("backfillDays no puede ser negativo.");
        }

        int numBlocks = backfillDays / WINDOW_DAYS;
        List<DateRange> blocks = new ArrayList<>(numBlocks);

        for (int i = 0; i < numBlocks; i++) {
            LocalDate end = today.minusDays((long) WINDOW_DAYS * i);
            LocalDate start = end.minusDays(WINDOW_DAYS - 1L);
            blocks.add(new DateRange(start, end));
        }

        return blocks;
    }
}