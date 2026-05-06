package org.ulpgc.dacd.weatherfeeder.controller.feeders;

import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;

import java.util.List;

public interface ClimateFeeder {
    List<WeatherEvent> fetch(String locationId);
}
