package org.ulpgc.dacd.marketfeeder.model.storers;

import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

import java.util.List;

public interface MarketStore {
    void initialize();
    void store(List<CommoditiesInfo> commoditiesInfoList);
}
