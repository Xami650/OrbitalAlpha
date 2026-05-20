package org.ulpgc.dacd.weatherfeeder.controller.chunker;

import org.junit.Before;
import org.junit.Test;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WeeklyDateChunkerTest {

    private WeeklyDateChunker chunker;

    @Before
    public void setUp() {
        chunker = new WeeklyDateChunker(7);
    }

    @Test
    public void currentWeekIsSevenDaysEndingToday() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        DateRange week = chunker.currentWeek(today);

        assertEquals(LocalDate.of(2026, 5, 9), week.start());
        assertEquals(today, week.end());
        assertEquals("20260509", week.startAsApiDate());
        assertEquals("20260515", week.endAsApiDate());
        assertEquals(6, ChronoUnit.DAYS.between(week.start(), week.end()));
    }

    @Test
    public void backfillProducesOneBlockPerWeek() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        assertEquals(0, chunker.backfillWeeks(today, 0).size());
        assertEquals(1, chunker.backfillWeeks(today, 1).size());
        assertEquals(2, chunker.backfillWeeks(today, 2).size());
        assertEquals(74, chunker.backfillWeeks(today, 74).size());
        assertEquals(520, chunker.backfillWeeks(today, 520).size());
    }

    @Test
    public void backfillFirstBlockIsCurrentWeekAndMostRecentFirst() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);

        assertEquals(LocalDate.of(2026, 5, 9), blocks.get(0).start());
        assertEquals(today, blocks.get(0).end());
        assertTrue("Debe estar ordenado mas-reciente -> mas-antiguo",
                blocks.get(0).end().isAfter(blocks.get(1).end()));
    }

    @Test
    public void backfillBlocksAreSevenDaysAndContiguousWithoutOverlap() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);

        for (int i = 0; i < blocks.size(); i++) {
            DateRange block = blocks.get(i);
            assertEquals("Bloque " + i + " debe tener 7 dias",
                    6, ChronoUnit.DAYS.between(block.start(), block.end()));
        }

        for (int i = 1; i < blocks.size(); i++) {
            DateRange prev = blocks.get(i - 1);
            DateRange curr = blocks.get(i);
            assertEquals("El bloque anterior debe empezar el dia siguiente al fin del actual",
                    prev.start(), curr.end().plusDays(1));
        }
    }

    @Test
    public void backfillLastBlockMatchesExpectedDate() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);
        DateRange last = blocks.getLast();

        assertEquals(LocalDate.of(2024, 12, 14), last.start());
        assertEquals(LocalDate.of(2024, 12, 20), last.end());
    }

    @Test
    public void backfillWithZeroWeeksReturnsEmpty() {
        assertEquals(0, chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 0).size());
    }

    @Test
    public void backfillBlocksAreAllDistinct() {
        List<DateRange> blocks = chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 74);

        for (int i = 0; i < blocks.size(); i++) {
            for (int j = i + 1; j < blocks.size(); j++) {
                assertNotEquals("Bloques " + i + " y " + j + " duplicados", blocks.get(i), blocks.get(j));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeNumWeeksIsRejected() {
        chunker.backfillWeeks(LocalDate.of(2026, 5, 15), -1);
    }

    @Test
    public void nullTodayIsRejected() {
        try {
            chunker.currentWeek(null);
            fail("Debe lanzar IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
