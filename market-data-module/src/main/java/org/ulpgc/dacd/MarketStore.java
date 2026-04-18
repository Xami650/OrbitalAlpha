package org.ulpgc.dacd;

import java.util.List;

public interface MarketStore {
    void initialize();
    void store(List<MarketData> marketDataList);
}