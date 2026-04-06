package org.ulpgc.dacd;

import java.util.List;

public interface MarketFeeder {
    List<MarketData> fetch(String symbol);
}
