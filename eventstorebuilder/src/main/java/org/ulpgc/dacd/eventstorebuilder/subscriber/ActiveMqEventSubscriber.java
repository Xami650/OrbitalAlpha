package org.ulpgc.dacd.eventstorebuilder.subscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveMqEventSubscriber implements EventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMqEventSubscriber.class);

    private final String brokerUrl;
    private final String clientId;
    private final List<String> topicNames;

    private Connection connection;
    private Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public ActiveMqEventSubscriber(String brokerUrl, String clientId, List<String> topicNames) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.topicNames = topicNames.stream()
                .map(String::trim)
                .filter(topicName -> !topicName.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void subscribe(EventMessageHandler handler) {
        try {
            openConnection();
            createSession();
            subscribeToTopics(handler);
            connection.start();
        } catch (JMSException e) {
            throw new java.lang.IllegalStateException("Error creating durable subscriber", e);
        }
    }

    private void openConnection() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        connection = factory.createConnection();
        connection.setClientID(clientId);
    }

    private void createSession() throws JMSException {
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    private void subscribeToTopics(EventMessageHandler handler) throws JMSException {
        for (String topicName : topicNames) {
            MessageConsumer consumer = createDurableConsumer(topicName);
            consumer.setMessageListener(message -> handleMessage(topicName, message, handler));
            consumers.add(consumer);

            logger.info("Durable subscription created for topic {}", topicName);
        }
    }

    private MessageConsumer createDurableConsumer(String topicName) throws JMSException {
        Topic topic = session.createTopic(topicName);
        String subscriptionName = "event-store-" + topicName;
        return session.createDurableSubscriber(topic, subscriptionName);
    }

    private void handleMessage(String topicName, Message message, EventMessageHandler handler) {
        if (!(message instanceof TextMessage textMessage)) {
            logger.warn("Ignored non-text message from topic {}", topicName);
            return;
        }

        try {
            String jsonEvent = textMessage.getText();
            handler.handle(topicName, jsonEvent);
            logger.info("Consumed event from topic {}", topicName);
        } catch (JMSException e) {
            logger.error("Error reading message from topic {}", topicName, e);
        } catch (RuntimeException e) {
            logger.error("Error handling event from topic {}", topicName, e);
        }
    }

    @Override
    public void close() {
        try {
            for (MessageConsumer consumer : consumers) {
                consumer.close();
            }

            if (session != null) {
                session.close();
            }

            if (connection != null) {
                connection.close();
            }

            logger.info("ActiveMQ durable subscriber closed.");
        } catch (JMSException e) {
            logger.error("Error closing durable subscriber", e);
        }
    }
}