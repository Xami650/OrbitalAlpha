package org.ulpgc.dacd.marketfeeder.controller.feeders.parsers;

import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.util.List;

public interface MarketParser {
    List<MarketEvent> parse(String symbol, String rawResponse);}
