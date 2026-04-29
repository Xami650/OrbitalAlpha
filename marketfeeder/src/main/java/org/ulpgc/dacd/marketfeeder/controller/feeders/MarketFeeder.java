package org.ulpgc.dacd.marketfeeder.controller.feeders;

import org.ulpgc.dacd.marketfeeder.model.MarketEvent;
import java.util.List;

public interface MarketFeeder {
    List<MarketEvent> getMarketData(String symbol);}
