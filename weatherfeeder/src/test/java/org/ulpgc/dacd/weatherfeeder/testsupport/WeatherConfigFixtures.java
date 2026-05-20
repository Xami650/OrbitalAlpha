package org.ulpgc.dacd.weatherfeeder.testsupport;

import org.ulpgc.dacd.weatherfeeder.model.BrokerConfig;
import org.ulpgc.dacd.weatherfeeder.model.NasaPowerApiConfig;
import org.ulpgc.dacd.weatherfeeder.model.ScheduleConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class WeatherConfigFixtures {

    private static final String DEFAULT_NASA_URL_TEMPLATE =
            "https://power.larc.nasa.gov/api/temporal/daily/point" +
                    "?parameters=PRECTOTCORR,GWETROOT,T2M_MAX,T2M_MIN" +
                    "&community=AG&longitude=%s&latitude=%s&start=%s&end=%s&format=JSON";

    private WeatherConfigFixtures() {
    }

    public static WeatherConfig defaults(WeatherMode mode, int backfillWeeks, Path producersFilePath) {
        return new WeatherConfig(
                mode,
                backfillWeeks,
                producersFilePath,
                "weatherfeeder",
                new BrokerConfig("tcp://localhost:61616", "Weather"),
                new NasaPowerApiConfig(DEFAULT_NASA_URL_TEMPLATE, 1000L),
                new ScheduleConfig(24, 7, 7)
        );
    }

    public static WeatherConfig defaults(WeatherMode mode, int backfillWeeks, String producersFilePath) {
        return defaults(mode, backfillWeeks, Paths.get(producersFilePath));
    }
}