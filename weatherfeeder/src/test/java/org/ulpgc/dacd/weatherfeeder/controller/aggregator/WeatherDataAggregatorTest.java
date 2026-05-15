package org.ulpgc.dacd.weatherfeeder.controller.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeatherDataAggregatorTest {

    private static final double DELTA = 1e-9;
    private static final String PERIOD_START = "20260509";
    private static final String PERIOD_END = "20260515";

    private WeatherDataAggregator aggregator;
    private NasaPowerClimateParser parser;
    private Producer producer;

    @Before
    public void setUp() {
        aggregator = new WeatherDataAggregator();
        parser = new NasaPowerClimateParser();
        producer = new Producer("GC-01", "Gran Canaria Sur", "TOMATO", 27.9, -15.6);
    }

    @Test
    public void averagesSevenValidDays() {
        String json = buildJson(
                new double[]{0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0},   // PRECTOTCORR avg = 3.0
                new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7},   // GWETROOT    avg = 0.4
                new double[]{20, 21, 22, 23, 24, 25, 26},          // T2M_MAX     avg = 23.0
                new double[]{10, 11, 12, 13, 14, 15, 16},          // T2M_MIN     avg = 13.0
                -999.0
        );

        List<WeatherEvent> events = parser.parse(json, producer, producer.id());
        assertEquals(7, events.size());

        Optional<WeatherAggregate> result = aggregator.aggregate(events, producer, PERIOD_START, PERIOD_END);

        assertTrue(result.isPresent());
        WeatherAggregate agg = result.get();
        assertEquals(7, agg.daysUsed());
        assertEquals(3.0, agg.avgPrecipitation(), DELTA);
        assertEquals(0.4, agg.avgRootZoneSoilWetness(), DELTA);
        assertEquals(23.0, agg.avgTemperatureMax(), DELTA);
        assertEquals(13.0, agg.avgTemperatureMin(), DELTA);
        assertEquals(producer.id(), agg.producerId());
        assertEquals(PERIOD_START, agg.periodStart());
        assertEquals(PERIOD_END, agg.periodEnd());
    }

    @Test
    public void skipsDayWithFillValueAndAveragesOverRemainingDays() {
        String json = buildJson(
                new double[]{2.0, 4.0, 6.0, -999.0, 8.0, 10.0, 12.0}, // 6 válidos, suma 42 → 7.0
                new double[]{0.2, 0.4, 0.6, 0.5, 0.8, 1.0, 1.2},      // día 4 se descarta
                new double[]{20, 22, 24, 26, 28, 30, 32},
                new double[]{10, 12, 14, 16, 18, 20, 22},
                -999.0
        );

        List<WeatherEvent> events = parser.parse(json, producer, producer.id());
        assertEquals(6, events.size());

        Optional<WeatherAggregate> result = aggregator.aggregate(events, producer, PERIOD_START, PERIOD_END);

        assertTrue(result.isPresent());
        WeatherAggregate agg = result.get();
        assertEquals(6, agg.daysUsed());
        assertEquals(7.0, agg.avgPrecipitation(), DELTA);
        assertEquals((0.2 + 0.4 + 0.6 + 0.8 + 1.0 + 1.2) / 6.0, agg.avgRootZoneSoilWetness(), DELTA);
        assertEquals((20 + 22 + 24 + 28 + 30 + 32) / 6.0, agg.avgTemperatureMax(), DELTA);
        assertEquals((10 + 12 + 14 + 18 + 20 + 22) / 6.0, agg.avgTemperatureMin(), DELTA);
    }

    @Test
    public void returnsEmptyWhenEventListIsEmpty() {
        Optional<WeatherAggregate> result = aggregator.aggregate(List.of(), producer, PERIOD_START, PERIOD_END);
        assertFalse(result.isPresent());
    }

    @Test
    public void returnsEmptyWhenEventListIsNull() {
        Optional<WeatherAggregate> result = aggregator.aggregate(null, producer, PERIOD_START, PERIOD_END);
        assertFalse(result.isPresent());
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