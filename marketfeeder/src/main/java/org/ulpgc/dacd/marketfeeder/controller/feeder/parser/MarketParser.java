package org.ulpgc.dacd.marketfeeder.controller.feeder.parser;

import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.util.List;

public interface MarketParser {
    List<MarketEvent> parse(String symbol, String rawResponse);}
