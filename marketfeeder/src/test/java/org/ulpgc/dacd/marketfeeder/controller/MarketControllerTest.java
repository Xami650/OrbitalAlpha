package org.ulpgc.dacd.marketfeeder.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.marketfeeder.controller.feeder.MarketFeeder;
import org.ulpgc.dacd.marketfeeder.controller.publisher.MarketPublisher;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketControllerTest {

    FakeMarketFeeder feeder;
    FakeMarketPublisher publisher;

    @BeforeEach
    void setUp() {
        feeder = new FakeMarketFeeder();
        publisher = new FakeMarketPublisher();
    }

    @Test
    void start_whenFeederReturnsEvents_publishesThem() throws InterruptedException {
        MarketEvent event = marketEvent("WEAT");
        feeder.eventsToReturn = List.of(event);

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        Thread.sleep(300);

        assertThat(feeder.requestedSymbols).contains("WEAT");
        assertThat(publisher.publishedEvents).contains(event);
    }

    @Test
    void start_whenFeederReturnsEmptyList_publishesNothing() throws InterruptedException {
        feeder.eventsToReturn = List.of();

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        Thread.sleep(300);

        assertThat(feeder.requestedSymbols).contains("WEAT");
        assertThat(publisher.publishedEvents).isEmpty();
    }

    @Test
    void start_withMultipleSymbols_processesAllSymbols() throws InterruptedException {
        feeder.eventsToReturn = List.of(marketEvent("GENERIC"));

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT", "CORN", "SOYB"),
                24,
                0
        );

        controller.start();

        Thread.sleep(500);

        assertThat(feeder.requestedSymbols)
                .contains("WEAT", "CORN", "SOYB");
    }

    @Test
    void start_whenFeederThrowsRuntimeException_doesNotPublishAnything() throws InterruptedException {
        feeder.failOnGetMarketData = true;

        MarketController controller = new MarketController(
                feeder,
                publisher,
                List.of("WEAT"),
                24,
                0
        );

        controller.start();

        Thread.sleep(300);

        assertThat(publisher.publishedEvents).isEmpty();
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

    private static class FakeMarketFeeder implements MarketFeeder {

        private final List<String> requestedSymbols = new ArrayList<>();
        private List<MarketEvent> eventsToReturn = List.of();
        private boolean failOnGetMarketData;

        @Override
        public List<MarketEvent> getMarketData(String symbol) {
            requestedSymbols.add(symbol);

            if (failOnGetMarketData) {
                throw new RuntimeException("API error");
            }

            return eventsToReturn;
        }
    }

    private static class FakeMarketPublisher implements MarketPublisher {

        private final List<MarketEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(MarketEvent event) {
            publishedEvents.add(event);
        }

        @Override
        public void close() {
        }
    }
}