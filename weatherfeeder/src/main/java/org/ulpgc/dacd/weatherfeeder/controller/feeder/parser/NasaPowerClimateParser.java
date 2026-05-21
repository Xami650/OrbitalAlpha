package org.ulpgc.dacd.weatherfeeder.controller.feeder.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NasaPowerClimateParser {

    private static final Logger logger = LoggerFactory.getLogger(NasaPowerClimateParser.class);
    private final String sourceSystem;

    public NasaPowerClimateParser(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("sourceSystem must not be blank.");
        }
        this.sourceSystem = sourceSystem;
    }

    private static final String KEY_PROPERTIES = "properties";
    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_HEADER = "header";
    private static final String KEY_FILL_VALUE = "fill_value";
    private static final String KEY_MESSAGES = "messages";

    private static final String PARAM_PRECTOTCORR = "PRECTOTCORR";
    private static final String PARAM_GWETROOT = "GWETROOT";
    private static final String PARAM_T2M_MAX = "T2M_MAX";
    private static final String PARAM_T2M_MIN = "T2M_MIN";

    public List<WeatherEvent> parse(String jsonResponse, Producer producer) {
        Optional<JsonObject> jsonObject = parseJsonObject(jsonResponse, producer.id());

        if (jsonObject.isEmpty()) {
            return List.of();
        }

        return parseValidJsonObject(jsonObject.get(), producer);
    }

    private Optional<JsonObject> parseJsonObject(String jsonResponse, String producerId) {
        try {
            return Optional.of(JsonParser.parseString(jsonResponse).getAsJsonObject());
        } catch (Exception e) {
            logger.error("NASA POWER response for {} is not valid JSON.", producerId, e);
            return Optional.empty();
        }
    }

    private List<WeatherEvent> parseValidJsonObject(JsonObject jsonObject, Producer producer) {
        logMessagesIfPresent(jsonObject, producer.id());

        Optional<JsonObject> parameterObject = extractParameterObject(jsonObject, producer.id());

        if (parameterObject.isEmpty()) {
            return List.of();
        }

        Double fillValue = extractFillValue(jsonObject);

        return parseParameters(parameterObject.get(), fillValue, producer);
    }

    private Optional<JsonObject> extractParameterObject(JsonObject jsonObject, String producerId) {
        Optional<JsonObject> properties = extractObject(jsonObject, KEY_PROPERTIES);

        if (properties.isEmpty()) {
            logger.error("NASA POWER response for {} does not contain '{}'.", producerId, KEY_PROPERTIES);
            return Optional.empty();
        }

        Optional<JsonObject> parameterObject = extractObject(properties.get(), KEY_PARAMETER);

        if (parameterObject.isEmpty()) {
            logger.error("NASA POWER response for {} does not contain '{}'.", producerId, KEY_PARAMETER);
            return Optional.empty();
        }

        return parameterObject;
    }

    private Optional<JsonObject> extractObject(JsonObject jsonObject, String key) {
        if (!jsonObject.has(key) || !jsonObject.get(key).isJsonObject()) {
            return Optional.empty();
        }

        return Optional.of(jsonObject.getAsJsonObject(key));
    }

    private void logMessagesIfPresent(JsonObject jsonObject, String producerId) {
        if (!jsonObject.has(KEY_MESSAGES) || !jsonObject.get(KEY_MESSAGES).isJsonArray()) {
            return;
        }

        JsonArray messages = jsonObject.getAsJsonArray(KEY_MESSAGES);

        if (!messages.isEmpty()) {
            logger.warn("NASA POWER returned messages for {}: {}", producerId, messages);
        }
    }

    private Double extractFillValue(JsonObject jsonObject) {
        Optional<JsonObject> header = extractObject(jsonObject, KEY_HEADER);

        if (header.isEmpty() || !header.get().has(KEY_FILL_VALUE)) {
            return null;
        }

        return readFillValue(header.get());
    }

    private Double readFillValue(JsonObject header) {
        try {
            return header.get(KEY_FILL_VALUE).getAsDouble();
        } catch (Exception e) {
            logger.warn("Could not read fill_value from response.", e);
            return null;
        }
    }

    private List<WeatherEvent> parseParameters(JsonObject parameterObject, Double fillValue, Producer producer) {
        if (!hasAllRequiredParameters(parameterObject)) {
            logger.error("Missing expected parameters in NASA POWER response for {}.", producer.id());
            return List.of();
        }

        ClimateSeries series = createClimateSeries(parameterObject);
        Instant capturedAt = Instant.now();

        return series.prectotcorr()
                .keySet()
                .stream()
                .sorted(Comparator.reverseOrder())
                .map(date -> parseWeatherEvent(date, producer, fillValue, capturedAt, series))
                .flatMap(Optional::stream)
                .toList();
    }

    private ClimateSeries createClimateSeries(JsonObject parameterObject) {
        return new ClimateSeries(
                parameterObject.getAsJsonObject(PARAM_PRECTOTCORR),
                parameterObject.getAsJsonObject(PARAM_GWETROOT),
                parameterObject.getAsJsonObject(PARAM_T2M_MAX),
                parameterObject.getAsJsonObject(PARAM_T2M_MIN)
        );
    }

    private Optional<WeatherEvent> parseWeatherEvent(
            String date,
            Producer producer,
            Double fillValue,
            Instant capturedAt,
            ClimateSeries series
    ) {
        if (!isCompleteDate(date, series)) {
            logger.warn("Incomplete date {} for {}. Skipped.", date, producer.id());
            return Optional.empty();
        }

        return createWeatherEventSafely(date, producer, fillValue, capturedAt, series);
    }

    private Optional<WeatherEvent> createWeatherEventSafely(
            String date,
            Producer producer,
            Double fillValue,
            Instant capturedAt,
            ClimateSeries series
    ) {
        Optional<ClimateMeasurements> measurements = readClimateMeasurementsSafely(
                date,
                producer,
                fillValue,
                series
        );

        if (measurements.isEmpty()) {
            return Optional.empty();
        }

        if (hasFillValues(measurements.get())) {
            logger.warn("Date {} of {} skipped due to NASA POWER fill values.", date, producer.id());
            return Optional.empty();
        }

        return Optional.of(buildWeatherEvent(date, producer, capturedAt, measurements.get()));
    }

    private Optional<ClimateMeasurements> readClimateMeasurementsSafely(
            String date,
            Producer producer,
            Double fillValue,
            ClimateSeries series
    ) {
        try {
            return Optional.of(readClimateMeasurements(date, fillValue, series));
        } catch (Exception e) {
            logger.warn("Day {} of {} ignored due to parsing error.", date, producer.id(), e);
            return Optional.empty();
        }
    }

    private ClimateMeasurements readClimateMeasurements(
            String date,
            Double fillValue,
            ClimateSeries series
    ) {
        return new ClimateMeasurements(
                readValue(series.prectotcorr(), date, fillValue),
                readValue(series.gwetroot(), date, fillValue),
                readValue(series.t2mMax(), date, fillValue),
                readValue(series.t2mMin(), date, fillValue)
        );
    }

    private boolean hasFillValues(ClimateMeasurements measurements) {
        return Double.isNaN(measurements.precipitation())
                || Double.isNaN(measurements.rootZoneSoilWetness())
                || Double.isNaN(measurements.temperatureMax())
                || Double.isNaN(measurements.temperatureMin());
    }

    private WeatherEvent buildWeatherEvent(
            String date,
            Producer producer,
            Instant capturedAt,
            ClimateMeasurements measurements
    ) {
        return new WeatherEvent(
                capturedAt,
                sourceSystem,
                producer.id(),
                producer.name(),
                producer.commodityType(),
                date,
                measurements.precipitation(),
                measurements.rootZoneSoilWetness(),
                measurements.temperatureMax(),
                measurements.temperatureMin()
        );
    }

    private boolean hasAllRequiredParameters(JsonObject parameterObject) {
        return isJsonObject(parameterObject, PARAM_PRECTOTCORR)
                && isJsonObject(parameterObject, PARAM_GWETROOT)
                && isJsonObject(parameterObject, PARAM_T2M_MAX)
                && isJsonObject(parameterObject, PARAM_T2M_MIN);
    }

    private boolean isJsonObject(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && jsonObject.get(key).isJsonObject();
    }

    private boolean isCompleteDate(String date, ClimateSeries series) {
        return hasValidDateValue(series.prectotcorr(), date)
                && hasValidDateValue(series.gwetroot(), date)
                && hasValidDateValue(series.t2mMax(), date)
                && hasValidDateValue(series.t2mMin(), date);
    }

    private boolean hasValidDateValue(JsonObject series, String date) {
        return series.has(date) && !series.get(date).isJsonNull();
    }

    private double readValue(JsonObject series, String date, Double fillValue) {
        double value = series.get(date).getAsDouble();

        if (fillValue != null && Double.compare(value, fillValue) == 0) {
            return Double.NaN;
        }

        return value;
    }

    private record ClimateSeries(
            JsonObject prectotcorr,
            JsonObject gwetroot,
            JsonObject t2mMax,
            JsonObject t2mMin
    ) {
    }

    private record ClimateMeasurements(
            double precipitation,
            double rootZoneSoilWetness,
            double temperatureMax,
            double temperatureMin
    ) {
    }
}