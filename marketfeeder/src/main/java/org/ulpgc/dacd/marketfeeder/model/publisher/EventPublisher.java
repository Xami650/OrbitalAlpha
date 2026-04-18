package org.ulpgc.dacd.marketfeeder.model.publisher;

import org.ulpgc.dacd.marketfeeder.model.events.MarketEvent;

public interface EventPublisher {
    void publishEvent(String topic, MarketEvent event);
    void close();
}
