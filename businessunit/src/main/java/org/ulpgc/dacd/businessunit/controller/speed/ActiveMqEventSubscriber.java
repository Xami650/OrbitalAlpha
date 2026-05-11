package org.ulpgc.dacd.businessunit.controller.speed;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
        this.topicNames = topicNames;
    }

    @Override
    public void subscribe(EventMessageHandler handler) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

            connection = factory.createConnection();
            connection.setClientID(clientId);

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            for (String topicName : topicNames) {
                Topic topic = session.createTopic(topicName);

                MessageConsumer consumer = session.createDurableSubscriber(
                        topic,
                        "business-unit-" + topicName
                );

                consumer.setMessageListener(message -> {
                    try {
                        if (message instanceof TextMessage textMessage) {
                            handler.handle(topicName, textMessage.getText());
                        }
                    } catch (Exception e) {
                        logger.error("Error processing message from topic {}", topicName, e);
                    }
                });

                consumers.add(consumer);
                logger.info("Business unit subscribed to topic {}", topicName);
            }

            connection.start();

        } catch (Exception e) {
            throw new RuntimeException("Error subscribing to ActiveMQ topics", e);
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

        } catch (Exception e) {
            throw new RuntimeException("Error closing ActiveMQ subscriber", e);
        }
    }
}