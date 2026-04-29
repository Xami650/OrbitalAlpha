package org.ulpgc.dacd.marketfeeder.controller.publisher;

import org.ulpgc.dacd.marketfeeder.model.MarketEvent;


public interface MarketPublisher extends AutoCloseable {
    void publish(MarketEvent event);
    @Override
    void close();
}