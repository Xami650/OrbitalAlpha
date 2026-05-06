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

public class ActiveMqEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMqEventPublisher.class);

    private static final String BROKER_URL = "tcp://localhost:61616";

    private final Connection connection;
    private final Session session;

    public ActiveMqEventPublisher() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            logger.info("Publisher conectado a ActiveMQ en {}.", BROKER_URL);

        } catch (JMSException e) {
            throw new IllegalStateException("No se pudo crear el publisher de ActiveMQ.", e);
        }
    }

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

    public void close() {
        closeSession();
        closeConnection();
    }

    private void closeSession() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (JMSException e) {
            logger.error("Error cerrando la sesión de ActiveMQ.", e);
        }
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            logger.error("Error cerrando la conexión de ActiveMQ.", e);
        }
    }
}