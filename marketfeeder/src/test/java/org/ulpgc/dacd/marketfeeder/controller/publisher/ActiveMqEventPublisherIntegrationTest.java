package org.ulpgc.dacd.marketfeeder.controller.publisher;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Disabled("Requires local ActiveMQ broker running on tcp://localhost:61616")
class ActiveMqEventPublisherIntegrationTest {

    private static final String BROKER_URL = "tcp://localhost:61616";

    @Test
    void publish_sendsMarketEventAsJsonMessage() throws Exception {
        String topicName = "TestCommodityPrice-" + UUID.randomUUID();

        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);

        try (Connection connection = factory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Topic topic = session.createTopic(topicName);
            MessageConsumer consumer = session.createConsumer(topic);

            connection.start();

            ActiveMqEventPublisher publisher =
                    new ActiveMqEventPublisher(BROKER_URL, topicName);

            publisher.publish(marketEvent());
            publisher.close();

            TextMessage message = (TextMessage) consumer.receive(1000);

            assertThat(message).isNotNull();
            assertThat(message.getText()).contains("\"ss\":\"AlphaVantage\"");
            assertThat(message.getText()).contains("\"symbol\":\"WEAT\"");
            assertThat(message.getText()).contains("\"priceTimestamp\":\"2026-04-24T00:00:00Z\"");
        }
    }

    @Test
    void close_afterCreation_doesNotThrowException() {
        String topicName = "TestCommodityPrice-" + UUID.randomUUID();

        try (ActiveMqEventPublisher publisher = new ActiveMqEventPublisher(BROKER_URL, topicName)) {
            assertThatCode(publisher::close)
                    .doesNotThrowAnyException();
        }
    }

    private MarketEvent marketEvent() {
        return new MarketEvent(
                Instant.parse("2026-04-29T10:00:00Z"),
                "AlphaVantage",
                "WEAT",
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