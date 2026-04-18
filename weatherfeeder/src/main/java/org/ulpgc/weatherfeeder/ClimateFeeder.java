package org.ulpgc.weatherfeeder;

import java.util.List;

public interface ClimateFeeder {
    List<ClimateData> fetch(String locationId);
}
