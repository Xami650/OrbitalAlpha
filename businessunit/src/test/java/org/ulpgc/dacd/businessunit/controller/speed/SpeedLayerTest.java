package org.ulpgc.dacd.businessunit.controller.speed;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.controller.batch.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SpeedLayerTest {

    private static final Gson gson = new Gson();

    @Test
    void ingestsEventIntoBatchDatamart() throws Exception {
        CapturingBatchDatamart batchDatamart = new CapturingBatchDatamart();
        CountDownLatch rebuildLatch = new CountDownLatch(1);
        ServingLayer servingLayer = countingServingLayer(rebuildLatch);
        FakeEventSubscriber subscriber = new FakeEventSubscriber();

        SpeedLayer speedLayer = new SpeedLayer(subscriber, batchDatamart, servingLayer);
        speedLayer.start();

        subscriber.emit("CommodityPrice", priceEventJson("WEAT", 5.80));

        rebuildLatch.await(10, TimeUnit.SECONDS);
        speedLayer.stop();

        assertThat(batchDatamart.saved).hasSize(1);
        HistoricalEvent saved = batchDatamart.saved.getFirst();
        assertThat(saved.topic()).isEqualTo("CommodityPrice");
        assertThat(saved.sourceSystem()).isEqualTo("AlphaVantage");
        assertThat(saved.rawJson()).contains("WEAT");
    }

    @Test
    void triggersServingRebuildAfterEvent() throws Exception {
        CapturingBatchDatamart batchDatamart = new CapturingBatchDatamart();
        CountDownLatch rebuildLatch = new CountDownLatch(1);
        AtomicInteger rebuildCount = new AtomicInteger();
        ServingLayer servingLayer = countingServingLayer(rebuildLatch, rebuildCount);
        FakeEventSubscriber subscriber = new FakeEventSubscriber();

        SpeedLayer speedLayer = new SpeedLayer(subscriber, batchDatamart, servingLayer);
        speedLayer.start();

        subscriber.emit("CommodityPrice", priceEventJson("WEAT", 5.80));

        rebuildLatch.await(10, TimeUnit.SECONDS);
        speedLayer.stop();

        assertThat(rebuildCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void debouncesMultipleRapidEvents() throws Exception {
        CapturingBatchDatamart batchDatamart = new CapturingBatchDatamart();
        CountDownLatch rebuildLatch = new CountDownLatch(1);
        AtomicInteger rebuildCount = new AtomicInteger();
        ServingLayer servingLayer = countingServingLayer(rebuildLatch, rebuildCount);
        FakeEventSubscriber subscriber = new FakeEventSubscriber();

        SpeedLayer speedLayer = new SpeedLayer(subscriber, batchDatamart, servingLayer);
        speedLayer.start();

        subscriber.emit("CommodityPrice", priceEventJson("WEAT", 5.80));
        subscriber.emit("CommodityPrice", priceEventJson("CORN", 4.50));
        subscriber.emit("Weather", weatherEventJson("WHEAT"));

        rebuildLatch.await(10, TimeUnit.SECONDS);
        speedLayer.stop();

        assertThat(batchDatamart.saved).hasSize(3);
        assertThat(rebuildCount.get()).isEqualTo(1);
    }

    @Test
    void closesSubscriberOnStop() {
        FakeEventSubscriber subscriber = new FakeEventSubscriber();
        CapturingBatchDatamart batchDatamart = new CapturingBatchDatamart();
        ServingLayer servingLayer = countingServingLayer(new CountDownLatch(1));

        SpeedLayer speedLayer = new SpeedLayer(subscriber, batchDatamart, servingLayer);
        speedLayer.start();
        speedLayer.stop();

        assertThat(subscriber.closed).isTrue();
    }

    private String priceEventJson(String symbol, double close) {
        return gson.toJson(Map.of(
                "ts", Instant.now().toString(),
                "ss", "AlphaVantage",
                "symbol", symbol,
                "close", close,
                "priceTimestamp", Instant.now().toString()
        ));
    }

    private String weatherEventJson(String commodityType) {
        return gson.toJson(Map.ofEntries(
                Map.entry("ts", Instant.now().toString()),
                Map.entry("ss", "weatherfeeder"),
                Map.entry("commodityType", commodityType),
                Map.entry("producerId", commodityType + "_1"),
                Map.entry("avgPrecipitation", 2.0),
                Map.entry("avgRootZoneSoilWetness", 0.5),
                Map.entry("avgTemperatureMax", 25.0),
                Map.entry("avgTemperatureMin", 10.0)
        ));
    }

    private ServingLayer countingServingLayer(CountDownLatch latch) {
        return countingServingLayer(latch, new AtomicInteger());
    }

    private ServingLayer countingServingLayer(CountDownLatch latch, AtomicInteger count) {
        return new ServingLayer(null, null, null, null, null) {
            @Override
            public void rebuild() {
                count.incrementAndGet();
                latch.countDown();
            }
        };
    }

    private static class FakeEventSubscriber implements EventSubscriber {
        private EventMessageHandler handler;
        boolean closed = false;

        @Override
        public void subscribe(EventMessageHandler handler) {
            this.handler = handler;
        }

        void emit(String topic, String json) {
            if (handler != null) handler.handle(topic, json);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class CapturingBatchDatamart implements BatchDatamart {
        final List<HistoricalEvent> saved = new ArrayList<>();

        @Override
        public void initialize() {}

        @Override
        public void clear() { saved.clear(); }

        @Override
        public void saveHistoricalEvent(HistoricalEvent event) {
            synchronized (saved) { saved.add(event); }
        }

        @Override
        public void saveHistoricalEvents(List<HistoricalEvent> events) {
            synchronized (saved) { saved.addAll(events); }
        }

        @Override
        public List<HistoricalEvent> findAllHistoricalEvents() {
            synchronized (saved) { return List.copyOf(saved); }
        }
    }
}
