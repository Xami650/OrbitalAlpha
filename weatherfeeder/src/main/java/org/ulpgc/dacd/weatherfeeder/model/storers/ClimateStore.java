package org.ulpgc.dacd.weatherfeeder.model.storers;

import org.ulpgc.dacd.weatherfeeder.model.ClimateData;

import java.util.List;

public interface ClimateStore {
    void store(List<ClimateData> climateDataList);
}
