package org.ulpgc.dacd.marketfeeder.model.publisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ulpgc.dacd.marketfeeder.model.events.MarketEvent;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.time.Instant;

public class ActiveMqEventPublisher implements EventPublisher {

    private final Connection connection;
    private final Session session;
    private final Gson gson;

    public ActiveMqEventPublisher(String brokerUrl) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            this.connection = factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            this.gson = new GsonBuilder()
                    .registerTypeAdapter(
                            Instant.class,
                            (JsonSerializer<Instant>) (src, unusedType, unusedContext) ->
                                    new JsonPrimitive(src.toString())
                    )
                    .create();
        } catch (JMSException e) {
            throw new RuntimeException("Error initializing ActiveMQ publisher", e);
        }
    }

    @Override
    public void publishEvent(String topic, MarketEvent event) {
        try {
            Destination destination = session.createTopic(topic);
            try (MessageProducer producer = session.createProducer(destination)) {
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                String eventJson = gson.toJson(event);
                TextMessage message = session.createTextMessage(eventJson);
                producer.send(message);
            }
        } catch (JMSException e) {
            throw new RuntimeException("Error publishing event to topic " + topic, e);
        }
    }

    @Override
    public void close() {
        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException("Error closing ActiveMQ publisher", e);
        }
    }
}
