package org.ulpgc.dacd.marketfeeder.model.parsers;

import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

import java.util.List;

public interface MarketParser {
    List<CommoditiesInfo> parse(String symbol, String rawResponse);
}
