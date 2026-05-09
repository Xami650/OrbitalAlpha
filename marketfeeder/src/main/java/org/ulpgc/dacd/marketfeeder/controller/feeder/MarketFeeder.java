package org.ulpgc.dacd.marketfeeder.controller.feeder;

import org.ulpgc.dacd.marketfeeder.model.MarketEvent;
import java.util.List;

public interface MarketFeeder {
    List<MarketEvent> getMarketData(String symbol);}
