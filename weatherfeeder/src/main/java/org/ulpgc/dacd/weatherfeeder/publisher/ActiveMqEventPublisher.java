package org.ulpgc.dacd.weatherfeeder.publisher;

import jakarta.jms.Connection;
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

    private static final String BROKER_URL = "tcp://localhost:61616";

    private final Connection connection;
    private final Session session;

    public ActiveMqEventPublisher() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new IllegalStateException("No se pudo crear el publisher de ActiveMQ", e);
        }
    }

    @Override
    public void publish(String topicName, String message) {
        try {
            Destination destination = session.createTopic(topicName);
            MessageProducer producer = session.createProducer(destination);
            TextMessage textMessage = session.createTextMessage(message);

            producer.send(textMessage);
            producer.close();

            logger.info("Evento publicado en topic {}.", topicName);
        } catch (JMSException e) {
            logger.error("Error publicando en topic {}.", topicName, e);
        }
    }

    @Override
    public void close() {
        try {
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            logger.error("Error cerrando publisher de ActiveMQ.", e);
        }
    }
}
