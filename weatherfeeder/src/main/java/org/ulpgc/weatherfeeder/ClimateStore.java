package org.ulpgc.weatherfeeder;

import java.util.List;

public interface ClimateStore {
    void store(List<ClimateData> climateDataList);
}
