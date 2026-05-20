package org.ulpgc.dacd.eventstorebuilder.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.eventstorebuilder.store.EventStore;
import org.ulpgc.dacd.eventstorebuilder.subscriber.EventSubscriber;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EventStoreControllerTest {

    FakeEventSubscriber subscriber;
    FakeEventStore eventStore;
    EventStoreController controller;

    @BeforeEach
    void setUp() {
        subscriber = new FakeEventSubscriber();
        eventStore = new FakeEventStore();
        controller = new EventStoreController(subscriber, eventStore);
    }

    @Test
    void start_subscribesToSubscriber() {
        controller.start();

        assertThat(subscriber.handler).isNotNull();
    }

    @Test
    void start_receivedEvent_delegatesToEventStore() {
        String jsonEvent = "{\"ts\":\"2026-04-29T22:56:26.952653Z\",\"ss\":\"AlphaVantage\"}";

        controller.start();
        subscriber.simulateMessage("CommodityPrice", jsonEvent);

        assertThat(eventStore.lastTopic).isEqualTo("CommodityPrice");
        assertThat(eventStore.lastJsonEvent).isEqualTo(jsonEvent);
    }

    @Test
    void start_multipleEvents_delegatesAllToEventStore() {
        String firstEvent = "{\"ts\":\"2026-04-29T10:00:00Z\",\"ss\":\"AlphaVantage\"}";
        String secondEvent = "{\"ts\":\"2026-04-29T11:00:00Z\",\"ss\":\"weatherfeeder\"}";

        controller.start();

        subscriber.simulateMessage("CommodityPrice", firstEvent);
        subscriber.simulateMessage("Weather", secondEvent);

        assertThat(eventStore.storedEvents)
                .extracting(StoredEvent::topic)
                .containsExactly("CommodityPrice", "Weather");

        assertThat(eventStore.storedEvents)
                .extracting(StoredEvent::jsonEvent)
                .containsExactly(firstEvent, secondEvent);
    }

    @Test
    void start_receivedEvent_keepsJsonExactlyAsReceived() {
        String jsonEvent = "{\"ss\":\"AlphaVantage\",\"ts\":\"2026-04-29T22:56:26.952653Z\",\"symbol\":\"WEAT\"}";

        controller.start();
        subscriber.simulateMessage("CommodityPrice", jsonEvent);

        assertThat(eventStore.lastJsonEvent).isEqualTo(jsonEvent);
    }

    @Test
    void start_storeThrowsException_doesNotPropagateToSubscriber() {
        eventStore.failOnStore = true;

        controller.start();

        assertThatCode(() -> subscriber.simulateMessage("CommodityPrice", "{}"))
                .doesNotThrowAnyException();
    }

    private static class FakeEventSubscriber implements EventSubscriber {

        private EventMessageHandler handler;

        @Override
        public void subscribe(EventMessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void close() {
        }

        void simulateMessage(String topic, String jsonEvent) {
            handler.handle(topic, jsonEvent);
        }
    }

    private static class FakeEventStore implements EventStore {

        private final List<StoredEvent> storedEvents = new ArrayList<>();
        private String lastTopic;
        private String lastJsonEvent;
        private boolean failOnStore;

        @Override
        public void store(String topic, String jsonEvent) {
            if (failOnStore) {
                throw new RuntimeException("disco lleno");
            }

            this.lastTopic = topic;
            this.lastJsonEvent = jsonEvent;
            this.storedEvents.add(new StoredEvent(topic, jsonEvent));
        }
    }

    private record StoredEvent(String topic, String jsonEvent) {
    }
}