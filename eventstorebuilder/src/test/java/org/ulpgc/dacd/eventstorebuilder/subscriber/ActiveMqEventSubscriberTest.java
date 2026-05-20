package org.ulpgc.dacd.eventstorebuilder.subscriber;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Disabled("Requires local ActiveMQ broker running on tcp://localhost:61616")
class ActiveMqEventSubscriberTest {

    private static final String BROKER_URL = "tcp://localhost:61616";

    @Test
    void subscribe_whenCommodityPriceMessageIsPublished_receivesMessageAndTopic() throws Exception {
        String topicName = "TestCommodityPrice-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        String jsonEvent = "{\"ts\":\"2026-04-29T10:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"WEAT\"}";

        List<String> receivedTopics = new ArrayList<>();
        List<String> receivedEvents = new ArrayList<>();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(topicName)
        );

        subscriber.subscribe((topic, event) -> {
            receivedTopics.add(topic);
            receivedEvents.add(event);
        });

        publishMessage(topicName, jsonEvent);

        Thread.sleep(500);

        subscriber.close();

        assertThat(receivedTopics).containsExactly(topicName);
        assertThat(receivedEvents).containsExactly(jsonEvent);
    }

    @Test
    void subscribe_whenWeatherMessageIsPublished_receivesMessageAndTopic() throws Exception {
        String topicName = "TestWeather-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        String jsonEvent = "{\"ts\":\"2026-04-29T11:00:00Z\",\"ss\":\"weatherfeeder\",\"producerId\":\"WHEAT_1\"}";

        List<String> receivedTopics = new ArrayList<>();
        List<String> receivedEvents = new ArrayList<>();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(topicName)
        );

        subscriber.subscribe((topic, event) -> {
            receivedTopics.add(topic);
            receivedEvents.add(event);
        });

        publishMessage(topicName, jsonEvent);

        Thread.sleep(500);

        subscriber.close();

        assertThat(receivedTopics).containsExactly(topicName);
        assertThat(receivedEvents).containsExactly(jsonEvent);
    }

    @Test
    void subscribe_whenMultipleCommodityMessagesArePublished_receivesAllMessagesInOrder() throws Exception {
        String topicName = "TestCommodityPrice-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        String firstEvent = "{\"ts\":\"2026-04-29T10:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"WEAT\"}";
        String secondEvent = "{\"ts\":\"2026-04-29T11:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"CORN\"}";
        String thirdEvent = "{\"ts\":\"2026-04-29T12:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"SOYB\"}";
        String fourthEvent = "{\"ts\":\"2026-04-29T13:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"JO\"}";
        String fifthEvent = "{\"ts\":\"2026-04-29T14:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"UNG\"}";

        List<String> receivedEvents = new ArrayList<>();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(topicName)
        );

        subscriber.subscribe((topic, jsonEvent) -> receivedEvents.add(jsonEvent));

        publishMessage(topicName, firstEvent);
        publishMessage(topicName, secondEvent);
        publishMessage(topicName, thirdEvent);
        publishMessage(topicName, fourthEvent);
        publishMessage(topicName, fifthEvent);

        Thread.sleep(1000);

        subscriber.close();

        assertThat(receivedEvents)
                .containsExactly(firstEvent, secondEvent, thirdEvent, fourthEvent, fifthEvent);
    }

    @Test
    void subscribe_toTwoTopics_receivesMessagesFromBothTopics() throws Exception {
        String commodityTopic = "TestCommodityPrice-" + UUID.randomUUID();
        String weatherTopic = "TestWeather-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        String commodityEvent = "{\"ts\":\"2026-04-29T10:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"WEAT\"}";
        String weatherEvent = "{\"ts\":\"2026-04-29T11:00:00Z\",\"ss\":\"weatherfeeder\",\"producerId\":\"WHEAT_1\"}";

        List<String> receivedTopics = new ArrayList<>();
        List<String> receivedEvents = new ArrayList<>();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(commodityTopic, weatherTopic)
        );

        subscriber.subscribe((topic, jsonEvent) -> {
            receivedTopics.add(topic);
            receivedEvents.add(jsonEvent);
        });

        publishMessage(commodityTopic, commodityEvent);
        publishMessage(weatherTopic, weatherEvent);

        Thread.sleep(700);

        subscriber.close();

        assertThat(receivedTopics)
                .contains(commodityTopic, weatherTopic);

        assertThat(receivedEvents)
                .contains(commodityEvent, weatherEvent);
    }

    @Test
    void subscribe_toMultipleTopics_receivesCommodityAndWeatherMessages() throws Exception {
        String commodityTopic = "TestCommodityPrice-" + UUID.randomUUID();
        String weatherTopic = "TestWeather-" + UUID.randomUUID();
        String backupTopic = "TestMarketBackup-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        String wheatEvent = "{\"ts\":\"2026-04-29T10:00:00Z\",\"ss\":\"AlphaVantage\",\"symbol\":\"WEAT\"}";
        String cornWeatherEvent = "{\"ts\":\"2026-04-29T11:00:00Z\",\"ss\":\"weatherfeeder\",\"producerId\":\"CORN_1\"}";
        String backupEvent = "{\"ts\":\"2026-04-29T12:00:00Z\",\"ss\":\"MarketBackup\",\"symbol\":\"SOYB\"}";

        List<String> receivedTopics = new ArrayList<>();
        List<String> receivedEvents = new ArrayList<>();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(commodityTopic, weatherTopic, backupTopic)
        );

        subscriber.subscribe((topic, jsonEvent) -> {
            receivedTopics.add(topic);
            receivedEvents.add(jsonEvent);
        });

        publishMessage(commodityTopic, wheatEvent);
        publishMessage(weatherTopic, cornWeatherEvent);
        publishMessage(backupTopic, backupEvent);

        Thread.sleep(1000);

        subscriber.close();

        assertThat(receivedTopics)
                .contains(commodityTopic, weatherTopic, backupTopic);

        assertThat(receivedEvents)
                .contains(wheatEvent, cornWeatherEvent, backupEvent);
    }

    @Test
    void close_afterSubscribe_doesNotThrowException() {
        String topicName = "TestTopic-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(topicName)
        );

        subscriber.subscribe((topic, jsonEvent) -> {
        });

        assertThatCode(subscriber::close)
                .doesNotThrowAnyException();
    }

    @Test
    void close_calledTwice_doesNotThrowException() {
        String topicName = "TestTopic-" + UUID.randomUUID();
        String clientId = "test-client-" + UUID.randomUUID();

        ActiveMqEventSubscriber subscriber = new ActiveMqEventSubscriber(
                BROKER_URL,
                clientId,
                List.of(topicName)
        );

        subscriber.subscribe((topic, jsonEvent) -> {
        });

        subscriber.close();

        assertThatCode(subscriber::close)
                .doesNotThrowAnyException();
    }

    private void publishMessage(String topicName, String jsonEvent) throws Exception {
        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);

        try (Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Topic topic = session.createTopic(topicName);
            MessageProducer producer = session.createProducer(topic);
            TextMessage message = session.createTextMessage(jsonEvent);

            producer.send(message);
        }
    }
}