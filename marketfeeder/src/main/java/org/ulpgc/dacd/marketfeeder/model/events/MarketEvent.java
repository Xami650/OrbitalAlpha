package org.ulpgc.dacd.marketfeeder.model.events;

import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

public record MarketEvent(
        String ts,
        String ss,
        CommoditiesInfo data
) {}