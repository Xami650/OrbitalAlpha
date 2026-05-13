package org.ulpgc.dacd.weatherfeeder.model;

import java.time.Instant;

public record WeatherEvent(
        Instant ts, /* Instante exacto en el cuál se hace la captura */
        String ss,
        String producerId,
        String producerName,
        String commodityType,
        String date, /* Fecha climática del dato diario de NASA POWER */
        double precipitation,
        double rootZoneSoilWetness,
        double temperatureMax,
        double temperatureMin
) {
}
