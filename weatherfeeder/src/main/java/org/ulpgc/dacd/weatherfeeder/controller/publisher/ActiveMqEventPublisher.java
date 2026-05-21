package org.ulpgc.dacd.weatherfeeder.controller.publisher;

import jakarta.jms.Connection;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveMqEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMqEventPublisher.class);

    private final String brokerUrl;
    private final Connection connection;
    private final Session session;
    private final Map<String, MessageProducer> producers = new ConcurrentHashMap<>();

    public ActiveMqEventPublisher(String brokerUrl) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalArgumentException("brokerUrl must not be blank.");
        }
        this.brokerUrl = brokerUrl;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            logger.info("Publisher connected to ActiveMQ at {}.", brokerUrl);
        } catch (JMSException e) {
            throw new IllegalStateException("Could not create ActiveMQ publisher.", e);
        }
    }

    @Override
    public void publish(String topicName, String message) {
        try {
            MessageProducer producer = producers.computeIfAbsent(topicName, this::createProducer);
            TextMessage textMessage = session.createTextMessage(message);
            producer.send(textMessage);
            logger.info("Event published to topic {}.", topicName);
        } catch (JMSException e) {
            logger.error("Error publishing to topic {}.", topicName, e);
        }
    }

    private MessageProducer createProducer(String topicName) {
        try {
            MessageProducer producer = session.createProducer(session.createTopic(topicName));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            return producer;
        } catch (JMSException e) {
            throw new IllegalStateException("Could not create producer for topic " + topicName, e);
        }
    }

    @Override
    public void close() {
        producers.values().forEach(this::closeProducer);
        producers.clear();
        closeSilently(session, "session");
        closeSilently(connection, "connection");
    }

    private void closeProducer(MessageProducer producer) {
        try {
            producer.close();
        } catch (JMSException e) {
            logger.warn("Could not close JMS producer.", e);
        }
    }

    private void closeSilently(AutoCloseable closeable, String description) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            logger.error("Error closing ActiveMQ {} ({}).", description, brokerUrl, e);
        }
    }
}