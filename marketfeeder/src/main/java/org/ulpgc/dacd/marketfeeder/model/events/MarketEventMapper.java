package org.ulpgc.dacd.marketfeeder.model.events;

import java.time.Instant;
import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

public class MarketEventMapper {

    public MarketEvent toEvent(CommoditiesInfo data) {
        return new MarketEvent(
                Instant.now().toString(),
                "market-feeder",
                data
        );
    }
}