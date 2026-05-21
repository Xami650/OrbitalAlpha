package org.ulpgc.dacd.marketfeeder.controller.publisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import jakarta.jms.Connection;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.time.Instant;

public class ActiveMqEventPublisher implements MarketPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMqEventPublisher.class);

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;
    private final Gson gson;
    private final String topicName;

    public ActiveMqEventPublisher(String brokerUrl, String topicName) {
        this.topicName = topicName;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            this.producer = session.createProducer(session.createTopic(topicName));
            this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            this.gson = new GsonBuilder()
                    .registerTypeAdapter(
                            Instant.class,
                            (JsonSerializer<Instant>) (src, unusedType, unusedContext) ->
                                    new JsonPrimitive(src.toString())
                    )
                    .create();

            logger.info("ActiveMQ Publisher connected to broker: {} on topic: {}", brokerUrl, topicName);
        } catch (JMSException e) {
            throw new RuntimeException("Error initializing ActiveMQ publisher", e);
        }
    }

    @Override
    public void publish(MarketEvent event) {
        try {
            String eventJson = gson.toJson(event);
            TextMessage message = session.createTextMessage(eventJson);
            producer.send(message);
        } catch (JMSException e) {
            logger.error("Error publishing event for {} to topic {}", event.symbol(), this.topicName, e);
        }
    }

    public void close() {
        try {
            if (producer != null) producer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
            logger.info("ActiveMQ connection closed.");
        } catch (JMSException e) {
            logger.error("Error closing ActiveMQ publisher", e);
        }
    }
}