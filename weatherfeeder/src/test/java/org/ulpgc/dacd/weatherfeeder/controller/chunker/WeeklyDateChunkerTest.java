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
        chunker = new WeeklyDateChunker();
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
    public void backfillProducesExpectedNumberOfBlocksFor520Days() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 520);

        assertEquals(74, blocks.size());
    }

    @Test
    public void backfillFirstBlockIsCurrentWeekAndMostRecentFirst() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 520);

        assertEquals(LocalDate.of(2026, 5, 9), blocks.get(0).start());
        assertEquals(today, blocks.get(0).end());
        assertTrue("Debe estar ordenado más-reciente → más-antiguo",
                blocks.get(0).end().isAfter(blocks.get(1).end()));
    }

    @Test
    public void backfillBlocksAreSevenDaysAndContiguousWithoutOverlap() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 520);

        for (int i = 0; i < blocks.size(); i++) {
            DateRange block = blocks.get(i);
            assertEquals("Bloque " + i + " debe tener 7 días",
                    6, ChronoUnit.DAYS.between(block.start(), block.end()));
        }

        for (int i = 1; i < blocks.size(); i++) {
            DateRange prev = blocks.get(i - 1);
            DateRange curr = blocks.get(i);
            assertEquals("El bloque anterior debe empezar el día siguiente al fin del actual",
                    prev.start(), curr.end().plusDays(1));
        }
    }

    @Test
    public void backfillLastBlockMatchesExpectedDate() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 520);
        DateRange last = blocks.get(blocks.size() - 1);

        assertEquals(LocalDate.of(2024, 11, 21), last.start());
        assertEquals(LocalDate.of(2024, 11, 27), last.end());
    }

    @Test
    public void backfillDiscardsRemainderSmallerThanSevenDays() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        assertEquals(0, chunker.backfillWeeks(today, 6).size());
        assertEquals(1, chunker.backfillWeeks(today, 7).size());
        assertEquals(1, chunker.backfillWeeks(today, 13).size());
        assertEquals(2, chunker.backfillWeeks(today, 14).size());
        assertEquals(2, chunker.backfillWeeks(today, 20).size());
    }

    @Test
    public void backfillWithZeroDaysReturnsEmpty() {
        assertEquals(0, chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 0).size());
    }

    @Test
    public void backfillBlocksAreAllDistinct() {
        List<DateRange> blocks = chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 520);

        for (int i = 0; i < blocks.size(); i++) {
            for (int j = i + 1; j < blocks.size(); j++) {
                assertNotEquals("Bloques " + i + " y " + j + " duplicados", blocks.get(i), blocks.get(j));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeBackfillDaysIsRejected() {
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