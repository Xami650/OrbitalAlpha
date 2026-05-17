package org.ulpgc.dacd.weatherfeeder.controller.feeder;

import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;

import java.util.List;

public interface ClimateFeeder {
    List<WeatherEvent> fetch(String producerId, DateRange range);
}