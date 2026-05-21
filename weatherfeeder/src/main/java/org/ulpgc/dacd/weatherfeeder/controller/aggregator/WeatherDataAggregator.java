package org.ulpgc.dacd.weatherfeeder.controller.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public class WeatherDataAggregator {

    private static final Logger logger = LoggerFactory.getLogger(WeatherDataAggregator.class);

    private final String sourceSystem;
    private final int expectedDays;

    public WeatherDataAggregator(String sourceSystem, int expectedDays) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("sourceSystem must not be blank.");
        }
        if (expectedDays <= 0) {
            throw new IllegalArgumentException("expectedDays must be > 0.");
        }
        this.sourceSystem = sourceSystem;
        this.expectedDays = expectedDays;
    }

    public Optional<WeatherAggregate> aggregate(
            List<WeatherEvent> events,
            Producer producer,
            String periodStart,
            String periodEnd
    ) {
        if (events == null || events.isEmpty()) {
            logger.warn("No valid events for {} in period {}-{}. Aggregate not published.",
                    producer.id(), periodStart, periodEnd);
            return Optional.empty();
        }

        warnIfIncompleteWindow(events.size(), producer.id(), periodStart, periodEnd);

        double avgPrecip = averageField(events, WeatherEvent::precipitation, "precipitation", producer.id());
        double avgWet = averageField(events, WeatherEvent::rootZoneSoilWetness, "rootZoneSoilWetness", producer.id());
        double avgTMax = averageField(events, WeatherEvent::temperatureMax, "temperatureMax", producer.id());
        double avgTMin = averageField(events, WeatherEvent::temperatureMin, "temperatureMin", producer.id());

        if (anyNaN(avgPrecip, avgWet, avgTMax, avgTMin)) {
            logger.warn("Not enough valid days to compute all averages for {}. Aggregate skipped.",
                    producer.id());
            return Optional.empty();
        }

        return Optional.of(new WeatherAggregate(
                Instant.now(),
                sourceSystem,
                producer.id(),
                producer.name(),
                producer.commodityType(),
                periodStart,
                periodEnd,
                events.size(),
                avgPrecip,
                avgWet,
                avgTMax,
                avgTMin
        ));
    }

    private void warnIfIncompleteWindow(int daysUsed, String producerId, String start, String end) {
        if (daysUsed < expectedDays) {
            logger.warn("Incomplete window for {}: {} valid days out of {} expected ({}-{}).",
                    producerId, daysUsed, expectedDays, start, end);
        }
    }

    private double averageField(
            List<WeatherEvent> events,
            ToDoubleFunction<WeatherEvent> extractor,
            String fieldName,
            String producerId
    ) {
        double sum = 0.0;
        int count = 0;

        for (WeatherEvent event : events) {
            double value = extractor.applyAsDouble(event);
            if (Double.isNaN(value)) {
                logger.warn("Invalid value in {} for {} on {}. Day skipped for that average.",
                        fieldName, producerId, event.date());
                continue;
            }
            sum += value;
            count++;
        }

        return count == 0 ? Double.NaN : sum / count;
    }

    private boolean anyNaN(double... values) {
        for (double v : values) {
            if (Double.isNaN(v)) {
                return true;
            }
        }
        return false;
    }
}