package org.ulpgc.dacd.weatherfeeder.controller.chunker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeeklyDateChunkerTest {

    private WeeklyDateChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new WeeklyDateChunker(7);
    }

    @Test
    void currentWeekIsSevenDaysEndingToday() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        DateRange week = chunker.currentWeek(today);

        assertThat(week.start()).isEqualTo(LocalDate.of(2026, 5, 9));
        assertThat(week.end()).isEqualTo(today);
        assertThat(week.startAsApiDate()).isEqualTo("20260509");
        assertThat(week.endAsApiDate()).isEqualTo("20260515");
        assertThat(ChronoUnit.DAYS.between(week.start(), week.end())).isEqualTo(6);
    }

    @Test
    void backfillProducesOneBlockPerWeek() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        assertThat(chunker.backfillWeeks(today, 0)).isEmpty();
        assertThat(chunker.backfillWeeks(today, 1)).hasSize(1);
        assertThat(chunker.backfillWeeks(today, 2)).hasSize(2);
        assertThat(chunker.backfillWeeks(today, 74)).hasSize(74);
        assertThat(chunker.backfillWeeks(today, 520)).hasSize(520);
    }

    @Test
    void backfillFirstBlockIsCurrentWeekAndMostRecentFirst() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);

        assertThat(blocks.get(0).start()).isEqualTo(LocalDate.of(2026, 5, 9));
        assertThat(blocks.get(0).end()).isEqualTo(today);
        assertThat(blocks.get(0).end()).isAfter(blocks.get(1).end());
    }

    @Test
    void backfillBlocksAreSevenDaysAndContiguousWithoutOverlap() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);

        for (int i = 0; i < blocks.size(); i++) {
            DateRange block = blocks.get(i);
            assertThat(ChronoUnit.DAYS.between(block.start(), block.end()))
                    .as("Block %d must be 7 days", i)
                    .isEqualTo(6);
        }

        for (int i = 1; i < blocks.size(); i++) {
            DateRange prev = blocks.get(i - 1);
            DateRange curr = blocks.get(i);
            assertThat(prev.start())
                    .as("Previous block must start the day after current block ends")
                    .isEqualTo(curr.end().plusDays(1));
        }
    }

    @Test
    void backfillLastBlockMatchesExpectedDate() {
        LocalDate today = LocalDate.of(2026, 5, 15);

        List<DateRange> blocks = chunker.backfillWeeks(today, 74);
        DateRange last = blocks.getLast();

        assertThat(last.start()).isEqualTo(LocalDate.of(2024, 12, 14));
        assertThat(last.end()).isEqualTo(LocalDate.of(2024, 12, 20));
    }

    @Test
    void backfillWithZeroWeeksReturnsEmpty() {
        assertThat(chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 0)).isEmpty();
    }

    @Test
    void backfillBlocksAreAllDistinct() {
        List<DateRange> blocks = chunker.backfillWeeks(LocalDate.of(2026, 5, 15), 74);

        assertThat(blocks).doesNotHaveDuplicates();
    }

    @Test
    void negativeNumWeeksIsRejected() {
        assertThatThrownBy(() -> chunker.backfillWeeks(LocalDate.of(2026, 5, 15), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullTodayIsRejected() {
        assertThatThrownBy(() -> chunker.currentWeek(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
