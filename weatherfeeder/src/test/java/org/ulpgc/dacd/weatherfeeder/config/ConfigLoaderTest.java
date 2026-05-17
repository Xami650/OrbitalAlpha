package org.ulpgc.dacd.weatherfeeder.config;

import org.junit.Test;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfigLoaderTest {

    @Test
    public void defaultsAreWeeklyAnd520Days() {
        WeatherConfig config = new ConfigLoader(new Properties(), name -> null).load();

        assertEquals(WeatherMode.WEEKLY, config.mode());
        assertEquals(520, config.backfillDays());
    }

    @Test
    public void propertiesOverrideDefaults() {
        Properties props = new Properties();
        props.setProperty("weather.mode", "BACKFILL");
        props.setProperty("weather.backfill.days", "210");

        WeatherConfig config = new ConfigLoader(props, name -> null).load();

        assertEquals(WeatherMode.BACKFILL, config.mode());
        assertEquals(210, config.backfillDays());
    }

    @Test
    public void envOverridesProperties() {
        Properties props = new Properties();
        props.setProperty("weather.mode", "WEEKLY");
        props.setProperty("weather.backfill.days", "210");

        Map<String, String> env = new HashMap<>();
        env.put("WEATHER_MODE", "BACKFILL");
        env.put("WEATHER_BACKFILL_DAYS", "70");

        WeatherConfig config = new ConfigLoader(props, env::get).load();

        assertEquals(WeatherMode.BACKFILL, config.mode());
        assertEquals(70, config.backfillDays());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidModeIsRejected() {
        Properties props = new Properties();
        props.setProperty("weather.mode", "PAST");

        new ConfigLoader(props, name -> null).load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveBackfillDaysIsRejected() {
        Properties props = new Properties();
        props.setProperty("weather.backfill.days", "0");

        new ConfigLoader(props, name -> null).load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonNumericBackfillDaysIsRejected() {
        Properties props = new Properties();
        props.setProperty("weather.backfill.days", "abc");

        new ConfigLoader(props, name -> null).load();
    }
}