package org.ulpgc.dacd.weatherfeeder.controller.publisher;

import jakarta.jms.Connection;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMqEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMqEventPublisher.class);

    private final String brokerUrl;
    private final Connection connection;
    private final Session session;

    public ActiveMqEventPublisher(String brokerUrl) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalArgumentException("brokerUrl no puede estar vacio.");
        }
        this.brokerUrl = brokerUrl;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            logger.info("Publisher conectado a ActiveMQ en {}.", brokerUrl);
        } catch (JMSException e) {
            throw new IllegalStateException("No se pudo crear el publisher de ActiveMQ.", e);
        }
    }

    @Override
    public void publish(String topicName, String message) {
        MessageProducer producer = null;
        try {
            Destination destination = session.createTopic(topicName);
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            TextMessage textMessage = session.createTextMessage(message);
            producer.send(textMessage);
            logger.info("Evento publicado en topic {}.", topicName);
        } catch (JMSException e) {
            logger.error("Error publicando en topic {}.", topicName, e);
        } finally {
            closeProducer(producer);
        }
    }

    private void closeProducer(MessageProducer producer) {
        if (producer == null) {
            return;
        }
        try {
            producer.close();
        } catch (JMSException e) {
            logger.warn("No se pudo cerrar el productor JMS.", e);
        }
    }

    @Override
    public void close() {
        closeSilently(session, "sesion");
        closeSilently(connection, "conexion");
    }

    private void closeSilently(AutoCloseable closeable, String description) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            logger.error("Error cerrando {} de ActiveMQ ({}).", description, brokerUrl, e);
        }
    }
}