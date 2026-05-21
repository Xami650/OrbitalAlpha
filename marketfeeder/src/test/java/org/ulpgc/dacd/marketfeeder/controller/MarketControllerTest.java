package org.ulpgc.dacd.marketfeeder.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.marketfeeder.controller.feeder.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.publisher.MarketPublisher;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

class MarketControllerTest {

    private MarketFeeder feeder;
    private MarketPublisher publisher;

    @BeforeEach
    void setUp() {
        feeder = mock(MarketFeeder.class);
        publisher = mock(MarketPublisher.class);
    }

    @Test
    void start_whenFeederReturnsEvents_publishesThem() {
        MarketEvent event = marketEvent("WEAT");
        when(feeder.getMarketData("WEAT")).thenReturn(List.of(event));

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        verify(feeder, timeout(1000)).getMarketData("WEAT");
        verify(publisher, timeout(1000)).publish(event);
        controller.close();
    }

    @Test
    void start_whenFeederReturnsEmptyList_publishesNothing() {
        when(feeder.getMarketData("WEAT")).thenReturn(List.of());

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        verify(feeder, timeout(1000)).getMarketData("WEAT");
        verify(publisher, never()).publish(any());
        controller.close();
    }

    @Test
    void start_withMultipleSymbols_processesAllSymbols() {
        MarketEvent event = marketEvent("GENERIC");
        when(feeder.getMarketData(anyString())).thenReturn(List.of(event));

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT", "CORN", "SOYB"),
                24,
                0
        );

        controller.start();

        verify(feeder, timeout(1000)).getMarketData("WEAT");
        verify(feeder, timeout(1000)).getMarketData("CORN");
        verify(feeder, timeout(1000)).getMarketData("SOYB");
        controller.close();
    }

    @Test
    void start_whenFeederThrowsRuntimeException_doesNotPublishAnything() {
        when(feeder.getMarketData(anyString())).thenThrow(new RuntimeException("API error"));

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        verify(feeder, timeout(1000)).getMarketData("WEAT");
        verify(publisher, never()).publish(any());
        controller.close();
    }

    private MarketEvent marketEvent(String symbol) {
        return new MarketEvent(
                Instant.parse("2026-04-29T10:00:00Z"),
                "AlphaVantage",
                symbol,
                Instant.parse("2026-04-24T00:00:00Z"),
                23.28,
                25.24,
                23.25,
                24.61,
                24.61,
                4054912L,
                0.0
        );
    }
}