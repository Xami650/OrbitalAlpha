package org.ulpgc.dacd.weatherfeeder.controller.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDataAggregatorTest {

    private static final double DELTA = 1e-9;
    private static final String PERIOD_START = "20260509";
    private static final String PERIOD_END = "20260515";

    private WeatherDataAggregator aggregator;
    private NasaPowerClimateParser parser;
    private Producer producer;

    @BeforeEach
    void setUp() {
        aggregator = new WeatherDataAggregator("weatherfeeder", 7);
        parser = new NasaPowerClimateParser("weatherfeeder");
        producer = new Producer("GC-01", "Gran Canaria Sur", "TOMATO", 27.9, -15.6);
    }

    @Test
    void averagesSevenValidDays() {
        String json = buildJson(
                new double[]{0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0},
                new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7},
                new double[]{20, 21, 22, 23, 24, 25, 26},
                new double[]{10, 11, 12, 13, 14, 15, 16},
                -999.0
        );

        List<WeatherEvent> events = parser.parse(json, producer);
        assertThat(events).hasSize(7);

        Optional<WeatherAggregate> result = aggregator.aggregate(events, producer, PERIOD_START, PERIOD_END);

        assertThat(result).isPresent();
        WeatherAggregate agg = result.get();
        assertThat(agg.daysUsed()).isEqualTo(7);
        assertThat(agg.avgPrecipitation()).isCloseTo(3.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgRootZoneSoilWetness()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgTemperatureMax()).isCloseTo(23.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgTemperatureMin()).isCloseTo(13.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.producerId()).isEqualTo(producer.id());
        assertThat(agg.periodStart()).isEqualTo(PERIOD_START);
        assertThat(agg.periodEnd()).isEqualTo(PERIOD_END);
    }

    @Test
    void skipsDayWithFillValueAndAveragesOverRemainingDays() {
        String json = buildJson(
                new double[]{2.0, 4.0, 6.0, -999.0, 8.0, 10.0, 12.0},
                new double[]{0.2, 0.4, 0.6, 0.5, 0.8, 1.0, 1.2},
                new double[]{20, 22, 24, 26, 28, 30, 32},
                new double[]{10, 12, 14, 16, 18, 20, 22},
                -999.0
        );

        List<WeatherEvent> events = parser.parse(json, producer);
        assertThat(events).hasSize(6);

        Optional<WeatherAggregate> result = aggregator.aggregate(events, producer, PERIOD_START, PERIOD_END);

        assertThat(result).isPresent();
        WeatherAggregate agg = result.get();
        assertThat(agg.daysUsed()).isEqualTo(6);
        assertThat(agg.avgPrecipitation()).isCloseTo(7.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgRootZoneSoilWetness()).isCloseTo((0.2 + 0.4 + 0.6 + 0.8 + 1.0 + 1.2) / 6.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgTemperatureMax()).isCloseTo((20 + 22 + 24 + 28 + 30 + 32) / 6.0, org.assertj.core.data.Offset.offset(DELTA));
        assertThat(agg.avgTemperatureMin()).isCloseTo((10 + 12 + 14 + 18 + 20 + 22) / 6.0, org.assertj.core.data.Offset.offset(DELTA));
    }

    @Test
    void returnsEmptyWhenEventListIsEmpty() {
        Optional<WeatherAggregate> result = aggregator.aggregate(List.of(), producer, PERIOD_START, PERIOD_END);
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenEventListIsNull() {
        Optional<WeatherAggregate> result = aggregator.aggregate(null, producer, PERIOD_START, PERIOD_END);
        assertThat(result).isEmpty();
    }

    private String buildJson(double[] prec, double[] wet, double[] tmax, double[] tmin, double fill) {
        String[] dates = {"20260509", "20260510", "20260511", "20260512", "20260513", "20260514", "20260515"};

        return "{"
                + "\"header\":{\"fill_value\":" + fill + "},"
                + "\"properties\":{\"parameter\":{"
                + "\"PRECTOTCORR\":" + asSeries(dates, prec) + ","
                + "\"GWETROOT\":" + asSeries(dates, wet) + ","
                + "\"T2M_MAX\":" + asSeries(dates, tmax) + ","
                + "\"T2M_MIN\":" + asSeries(dates, tmin)
                + "}}}";
    }

    private String asSeries(String[] dates, double[] values) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < dates.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(dates[i]).append("\":").append(values[i]);
        }
        sb.append("}");
        return sb.toString();
    }
}
