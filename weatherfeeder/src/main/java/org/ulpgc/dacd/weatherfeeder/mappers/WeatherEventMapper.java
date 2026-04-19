package org.ulpgc.dacd.weatherfeeder.mappers;

import org.ulpgc.dacd.weatherfeeder.events.WeatherEvent;
import org.ulpgc.dacd.weatherfeeder.model.ClimateData;
import java.time.Instant;

public class WeatherEventMapper {

    private static final String SOURCE_SYSTEM = "weatherfeeder";

    public WeatherEvent map(ClimateData data) {
        return new WeatherEvent(
                Instant.now().toString(),
                SOURCE_SYSTEM,
                data.producerId(),
                data.producerName(),
                data.commodityType(),
                data.date(),
                data.precipitation(),
                data.rootZoneSoilWetness(),
                data.temperatureMax(),
                data.temperatureMin()
        );
    }
}
