package org.ulpgc.dacd.weatherfeeder.controller.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherAggregate;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public class WeatherDataAggregator {

    private static final Logger logger = LoggerFactory.getLogger(WeatherDataAggregator.class);

    private static final String SOURCE_SYSTEM = "weatherfeeder";
    private static final int EXPECTED_DAYS = 7;

    public Optional<WeatherAggregate> aggregate(
            List<WeatherEvent> events,
            Producer producer,
            String periodStart,
            String periodEnd
    ) {
        if (events == null || events.isEmpty()) {
            logger.warn("Sin eventos válidos para {} en el periodo {}-{}. No se publica agregado.",
                    producer.id(), periodStart, periodEnd);
            return Optional.empty();
        }

        warnIfIncompleteWindow(events.size(), producer.id(), periodStart, periodEnd);

        double avgPrecip = averageField(events, WeatherEvent::precipitation, "precipitation", producer.id());
        double avgWet = averageField(events, WeatherEvent::rootZoneSoilWetness, "rootZoneSoilWetness", producer.id());
        double avgTMax = averageField(events, WeatherEvent::temperatureMax, "temperatureMax", producer.id());
        double avgTMin = averageField(events, WeatherEvent::temperatureMin, "temperatureMin", producer.id());

        if (anyNaN(avgPrecip, avgWet, avgTMax, avgTMin)) {
            logger.warn("No hay días válidos suficientes para calcular todas las medias de {}. Se omite el agregado.",
                    producer.id());
            return Optional.empty();
        }

        WeatherAggregate aggregate = new WeatherAggregate(
                Instant.now(),
                SOURCE_SYSTEM,
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
        );

        return Optional.of(aggregate);
    }

    private void warnIfIncompleteWindow(int daysUsed, String producerId, String start, String end) {
        if (daysUsed < EXPECTED_DAYS) {
            logger.warn("Ventana incompleta para {}: {} días válidos de {} esperados ({}-{}).",
                    producerId, daysUsed, EXPECTED_DAYS, start, end);
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
                logger.warn("Valor inválido en {} para {} en {}. Día omitido para esa media.",
                        fieldName, producerId, event.date());
                continue;
            }

            sum += value;
            count++;
        }

        if (count == 0) {
            return Double.NaN;
        }

        return sum / count;
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