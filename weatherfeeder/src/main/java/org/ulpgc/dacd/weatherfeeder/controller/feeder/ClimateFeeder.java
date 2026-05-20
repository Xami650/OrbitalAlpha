package org.ulpgc.dacd.weatherfeeder.controller.feeder;

import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.util.List;

public interface ClimateFeeder {
    List<WeatherEvent> fetch(Producer producer, DateRange range);
}