package org.ulpgc.dacd.weatherfeeder.model.feeders;

import org.ulpgc.dacd.weatherfeeder.model.ClimateData;

import java.util.List;

public interface ClimateFeeder {
    List<ClimateData> fetch(String locationId);
}
