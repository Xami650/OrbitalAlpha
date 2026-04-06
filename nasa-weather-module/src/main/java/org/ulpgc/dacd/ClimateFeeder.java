package org.ulpgc.dacd;

import java.util.List;

public interface ClimateFeeder {
    List<ClimateData> fetch(String locationId);
}
