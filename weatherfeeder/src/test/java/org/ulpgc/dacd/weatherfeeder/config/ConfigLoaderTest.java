package org.ulpgc.dacd.weatherfeeder.config;

import org.junit.Test;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfigLoaderTest {

    private static Properties validProperties() {
        Properties props = new Properties();
        props.setProperty("weather.mode", "WEEKLY");
        props.setProperty("weather.backfill.days", "520");
        props.setProperty("weather.producers.file", "config/producers.csv");
        props.setProperty("weather.source.system", "weatherfeeder");
        props.setProperty("broker.url", "tcp://localhost:61616");
        props.setProperty("broker.topic.weather", "Weather");
        props.setProperty("nasa.api.url.template", "https://example.com?lon=%s&lat=%s&start=%s&end=%s");
        props.setProperty("nasa.api.rate.limit.pause.ms", "1000");
        props.setProperty("schedule.collection.interval.hours", "24");
        props.setProperty("schedule.window.days", "7");
        props.setProperty("schedule.expected.days", "7");
        return props;
    }

    @Test
    public void loadsAllPropertiesCorrectly() {
        WeatherConfig config = new ConfigLoader(validProperties()).load();

        assertEquals(WeatherMode.WEEKLY, config.mode());
        assertEquals(520, config.backfillDays());
        assertEquals("weatherfeeder", config.sourceSystem());
        assertEquals("tcp://localhost:61616", config.broker().url());
        assertEquals("Weather", config.broker().weatherTopic());
        assertEquals(1000L, config.api().rateLimitPauseMs());
        assertEquals(24, config.schedule().collectionIntervalHours());
        assertEquals(7, config.schedule().windowDays());
        assertEquals(7, config.schedule().expectedDays());
    }

    @Test
    public void backfillModeIsParsed() {
        Properties props = validProperties();
        props.setProperty("weather.mode", "BACKFILL");
        props.setProperty("weather.backfill.days", "210");

        WeatherConfig config = new ConfigLoader(props).load();

        assertEquals(WeatherMode.BACKFILL, config.mode());
        assertEquals(210, config.backfillDays());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidModeIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.mode", "PAST");
        new ConfigLoader(props).load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveBackfillDaysIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.backfill.days", "0");
        new ConfigLoader(props).load();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonNumericBackfillDaysIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.backfill.days", "abc");
        new ConfigLoader(props).load();
    }

    @Test(expected = IllegalStateException.class)
    public void missingRequiredPropertyIsRejected() {
        Properties props = validProperties();
        props.remove("broker.url");
        new ConfigLoader(props).load();
    }
}