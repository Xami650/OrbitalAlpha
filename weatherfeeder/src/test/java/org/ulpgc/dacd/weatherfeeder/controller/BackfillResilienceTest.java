package org.ulpgc.dacd.weatherfeeder.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ulpgc.dacd.weatherfeeder.controller.aggregator.WeatherDataAggregator;
import org.ulpgc.dacd.weatherfeeder.controller.chunker.WeeklyDateChunker;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.EventPublisher;
import org.ulpgc.dacd.weatherfeeder.controller.serializer.GsonEventSerializer;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;
import org.ulpgc.dacd.weatherfeeder.testsupport.WeatherConfigFixtures;

import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackfillResilienceTest {

    private static final String PRODUCER_ID = "GC-01";
    private static final Producer PRODUCER = new Producer(PRODUCER_ID, "Gran Canaria", "TOMATO", 27.9, -15.6);

    private ClimateFeeder feeder;
    private EventPublisher publisher;
    private ProducersInfo producersInfo;

    @BeforeEach
    void setUp() {
        feeder = mock(ClimateFeeder.class);
        publisher = mock(EventPublisher.class);
        producersInfo = mock(ProducersInfo.class);

        when(producersInfo.getAllIds()).thenReturn(List.of(PRODUCER_ID));
        when(producersInfo.getById(PRODUCER_ID)).thenReturn(PRODUCER);
    }

    private WeatherConfig backfillConfig(int weeks) {
        return WeatherConfigFixtures.defaults(WeatherMode.BACKFILL, weeks, Paths.get("dummy"));
    }

    private ClimateController newController(WeatherConfig config) {
        WeatherDataAggregator aggregator = new WeatherDataAggregator(
                config.sourceSystem(),
                config.schedule().expectedDays()
        );
        WeeklyDateChunker chunker = new WeeklyDateChunker(config.schedule().windowDays());
        return new ClimateController(
                feeder, producersInfo, publisher,
                aggregator, chunker,
                new GsonEventSerializer(),
                config,
                Clock.system(ZoneOffset.UTC)
        );
    }

    private void runAndClose(ClimateController controller) {
        try {
            controller.start();
        } finally {
            controller.close();
        }
    }

    @Test
    void backfillContinuesWhenOneBlockFails() {
        when(feeder.fetch(eq(PRODUCER), any(DateRange.class)))
                .thenReturn(sevenValidDays())
                .thenReturn(sevenValidDays())
                .thenThrow(new RuntimeException("NASA 500"))
                .thenReturn(sevenValidDays())
                .thenReturn(sevenValidDays());

        runAndClose(newController(backfillConfig(5)));

        verify(feeder, times(5)).fetch(eq(PRODUCER), any(DateRange.class));
        verify(publisher, times(4)).publish(eq("Weather"), anyString());
        verify(publisher).close();
    }

    @Test
    void backfillPublishesNothingWhenAllBlocksFail() {
        when(feeder.fetch(eq(PRODUCER), any(DateRange.class)))
                .thenThrow(new RuntimeException("NASA down"));

        runAndClose(newController(backfillConfig(3)));

        verify(feeder, times(3)).fetch(eq(PRODUCER), any(DateRange.class));
        verify(publisher, times(0)).publish(anyString(), anyString());
    }

    @Test
    void backfillEmitsMessagesInMostRecentFirstOrder() {
        when(feeder.fetch(eq(PRODUCER), any(DateRange.class)))
                .thenReturn(sevenValidDays());

        runAndClose(newController(backfillConfig(3)));

        ArgumentCaptor<DateRange> rangeCaptor = ArgumentCaptor.forClass(DateRange.class);
        verify(feeder, atLeastOnce()).fetch(eq(PRODUCER), rangeCaptor.capture());

        List<DateRange> ranges = rangeCaptor.getAllValues();
        assertThat(ranges).hasSize(3);
        for (int i = 1; i < ranges.size(); i++) {
            assertThat(ranges.get(i - 1).end()).isAfter(ranges.get(i).end());
        }
    }

    private List<WeatherEvent> sevenValidDays() {
        LocalDate base = LocalDate.of(2026, 5, 9);
        return IntStream.range(0, 7)
                .mapToObj(i -> new WeatherEvent(
                        Instant.parse("2026-05-15T00:00:00Z"),
                        "weatherfeeder",
                        PRODUCER.id(),
                        PRODUCER.name(),
                        PRODUCER.commodityType(),
                        base.plusDays(i).toString().replace("-", ""),
                        1.0 + i,
                        0.3,
                        24.0,
                        14.0
                ))
                .toList();
    }
}
