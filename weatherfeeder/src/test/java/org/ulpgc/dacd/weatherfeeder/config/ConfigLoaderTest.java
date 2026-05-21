package org.ulpgc.dacd.weatherfeeder.config;

import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import org.ulpgc.dacd.weatherfeeder.model.WeatherMode;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    private static Properties validProperties() {
        Properties props = new Properties();
        props.setProperty("weather.mode", "WEEKLY");
        props.setProperty("weather.backfill.weeks", "520");
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
    void loadsAllPropertiesCorrectly() {
        WeatherConfig config = new ConfigLoader(validProperties()).load();

        assertThat(config.mode()).isEqualTo(WeatherMode.WEEKLY);
        assertThat(config.backfillWeeks()).isEqualTo(520);
        assertThat(config.sourceSystem()).isEqualTo("weatherfeeder");
        assertThat(config.broker().url()).isEqualTo("tcp://localhost:61616");
        assertThat(config.broker().weatherTopic()).isEqualTo("Weather");
        assertThat(config.api().rateLimitPauseMs()).isEqualTo(1000L);
        assertThat(config.schedule().collectionIntervalHours()).isEqualTo(24);
        assertThat(config.schedule().windowDays()).isEqualTo(7);
        assertThat(config.schedule().expectedDays()).isEqualTo(7);
    }

    @Test
    void backfillModeIsParsed() {
        Properties props = validProperties();
        props.setProperty("weather.mode", "BACKFILL");
        props.setProperty("weather.backfill.weeks", "210");

        WeatherConfig config = new ConfigLoader(props).load();

        assertThat(config.mode()).isEqualTo(WeatherMode.BACKFILL);
        assertThat(config.backfillWeeks()).isEqualTo(210);
    }

    @Test
    void invalidModeIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.mode", "PAST");

        assertThatThrownBy(() -> new ConfigLoader(props).load())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonPositiveBackfillWeeksIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.backfill.weeks", "0");

        assertThatThrownBy(() -> new ConfigLoader(props).load())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonNumericBackfillWeeksIsRejected() {
        Properties props = validProperties();
        props.setProperty("weather.backfill.weeks", "abc");

        assertThatThrownBy(() -> new ConfigLoader(props).load())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingRequiredPropertyIsRejected() {
        Properties props = validProperties();
        props.remove("broker.url");

        assertThatThrownBy(() -> new ConfigLoader(props).load())
                .isInstanceOf(IllegalStateException.class);
    }
}
