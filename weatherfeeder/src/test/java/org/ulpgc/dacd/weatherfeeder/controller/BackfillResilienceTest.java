package org.ulpgc.dacd.weatherfeeder.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.ClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.ActiveMqEventPublisher;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackfillResilienceTest {

    private static final String PRODUCER_ID = "GC-01";
    private static final Producer PRODUCER = new Producer(PRODUCER_ID, "Gran Canaria", "TOMATO", 27.9, -15.6);

    private ClimateFeeder feeder;
    private ActiveMqEventPublisher publisher;
    private ProducersInfo producersInfo;

    @Before
    public void setUp() {
        feeder = mock(ClimateFeeder.class);
        publisher = mock(ActiveMqEventPublisher.class);
        producersInfo = mock(ProducersInfo.class);

        when(producersInfo.getAllIds()).thenReturn(List.of(PRODUCER_ID));
        when(producersInfo.getById(PRODUCER_ID)).thenReturn(PRODUCER);
    }

    @Test
    public void backfillContinuesWhenOneBlockFails() {
        when(feeder.fetch(eq(PRODUCER_ID), any(DateRange.class)))
                .thenReturn(sevenValidDays())   // bloque 0
                .thenReturn(sevenValidDays())   // bloque 1
                .thenThrow(new RuntimeException("NASA 500"))   // bloque 2 falla
                .thenReturn(sevenValidDays())   // bloque 3
                .thenReturn(sevenValidDays());  // bloque 4

        WeatherConfig config = new WeatherConfig(
                WeatherMode.BACKFILL,
                35,
                Paths.get("dummy")
        );

        ClimateController controller = new ClimateController(feeder, producersInfo, publisher, config);
        controller.start();

        verify(feeder, times(5)).fetch(eq(PRODUCER_ID), any(DateRange.class));
        verify(publisher, times(4)).publish(eq("Weather"), anyString());
        verify(publisher).close();
    }

    @Test
    public void backfillPublishesNothingWhenAllBlocksFail() {
        when(feeder.fetch(eq(PRODUCER_ID), any(DateRange.class)))
                .thenThrow(new RuntimeException("NASA down"));

        WeatherConfig config = new WeatherConfig(
                WeatherMode.BACKFILL,
                21,
                Paths.get("dummy")
        );

        ClimateController controller = new ClimateController(feeder, producersInfo, publisher, config);
        controller.start();

        verify(feeder, times(3)).fetch(eq(PRODUCER_ID), any(DateRange.class));
        verify(publisher, times(0)).publish(anyString(), anyString());
    }

    @Test
    public void backfillEmitsMessagesInMostRecentFirstOrder() {
        when(feeder.fetch(eq(PRODUCER_ID), any(DateRange.class)))
                .thenReturn(sevenValidDays());

        WeatherConfig config = new WeatherConfig(
                WeatherMode.BACKFILL,
                21,
                Paths.get("dummy")
        );

        ClimateController controller = new ClimateController(feeder, producersInfo, publisher, config);
        controller.start();

        ArgumentCaptor<DateRange> rangeCaptor = ArgumentCaptor.forClass(DateRange.class);
        verify(feeder, atLeastOnce()).fetch(eq(PRODUCER_ID), rangeCaptor.capture());

        List<DateRange> ranges = rangeCaptor.getAllValues();
        assertEquals(3, ranges.size());
        for (int i = 1; i < ranges.size(); i++) {
            if (!ranges.get(i - 1).end().isAfter(ranges.get(i).end())) {
                throw new AssertionError("El orden no es más-reciente → más-antiguo");
            }
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