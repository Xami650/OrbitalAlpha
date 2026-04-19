package org.ulpgc.dacd.weatherfeeder.model.parsers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ClimateData;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NasaPowerClimateParser {

    private static final Logger logger = LoggerFactory.getLogger(NasaPowerClimateParser.class);

    private static final String KEY_PROPERTIES = "properties";
    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_HEADER = "header";
    private static final String KEY_FILL_VALUE = "fill_value";
    private static final String KEY_MESSAGES = "messages";

    private static final String PARAM_PRECTOTCORR = "PRECTOTCORR";
    private static final String PARAM_GWETROOT = "GWETROOT";
    private static final String PARAM_T2M_MAX = "T2M_MAX";
    private static final String PARAM_T2M_MIN = "T2M_MIN";

    public List<ClimateData> parse(String jsonResponse, Producer producer, String producerId) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            logMessagesIfPresent(jsonObject, producerId);

            if (!jsonObject.has(KEY_PROPERTIES) || !jsonObject.get(KEY_PROPERTIES).isJsonObject()) {
                logger.error("La respuesta de NASA POWER para {} no contiene '{}'.", producerId, KEY_PROPERTIES);
                return Collections.emptyList();
            }

            JsonObject properties = jsonObject.getAsJsonObject(KEY_PROPERTIES);

            if (!properties.has(KEY_PARAMETER) || !properties.get(KEY_PARAMETER).isJsonObject()) {
                logger.error("La respuesta de NASA POWER para {} no contiene '{}'.", producerId, KEY_PARAMETER);
                return Collections.emptyList();
            }

            Double fillValue = extractFillValue(jsonObject);
            JsonObject parameterObject = properties.getAsJsonObject(KEY_PARAMETER);

            return parseParameters(parameterObject, fillValue, producer);
        } catch (Exception e) {
            logger.error("Error parseando la respuesta de NASA POWER para {}.", producerId, e);
            return Collections.emptyList();
        }
    }

    private void logMessagesIfPresent(JsonObject jsonObject, String producerId) {
        if (!jsonObject.has(KEY_MESSAGES) || !jsonObject.get(KEY_MESSAGES).isJsonArray()) {
            return;
        }

        JsonArray messages = jsonObject.getAsJsonArray(KEY_MESSAGES);

        if (!messages.isEmpty()) {
            logger.warn("NASA POWER devolvió mensajes para {}: {}", producerId, messages);
        }
    }

    private Double extractFillValue(JsonObject jsonObject) {
        if (!jsonObject.has(KEY_HEADER) || !jsonObject.get(KEY_HEADER).isJsonObject()) {
            return null;
        }

        JsonObject header = jsonObject.getAsJsonObject(KEY_HEADER);

        if (!header.has(KEY_FILL_VALUE)) {
            return null;
        }

        try {
            return header.get(KEY_FILL_VALUE).getAsDouble();
        } catch (Exception e) {
            logger.warn("No se pudo leer fill_value de la respuesta.", e);
            return null;
        }
    }

    private List<ClimateData> parseParameters(JsonObject parameterObject, Double fillValue, Producer producer) {
        if (!hasAllRequiredParameters(parameterObject)) {
            logger.error("Faltan parámetros esperados en la respuesta de NASA POWER para {}.", producer.id());
            return Collections.emptyList();
        }

        JsonObject prectotcorrSeries = parameterObject.getAsJsonObject(PARAM_PRECTOTCORR);
        JsonObject gwetrootSeries = parameterObject.getAsJsonObject(PARAM_GWETROOT);
        JsonObject t2mMaxSeries = parameterObject.getAsJsonObject(PARAM_T2M_MAX);
        JsonObject t2mMinSeries = parameterObject.getAsJsonObject(PARAM_T2M_MIN);

        List<String> dates = new ArrayList<>(prectotcorrSeries.keySet());
        dates.sort(Collections.reverseOrder());

        List<ClimateData> result = new ArrayList<>();

        for (String date : dates) {
            if (!isCompleteDate(date, prectotcorrSeries, gwetrootSeries, t2mMaxSeries, t2mMinSeries)) {
                logger.warn("Fecha {} incompleta para {}. Se omite.", date, producer.id());
                continue;
            }

            try {
                double precipitation = readValue(prectotcorrSeries, date, fillValue);
                double rootZoneSoilWetness = readValue(gwetrootSeries, date, fillValue);
                double temperatureMax = readValue(t2mMaxSeries, date, fillValue);
                double temperatureMin = readValue(t2mMinSeries, date, fillValue);

                if (Double.isNaN(precipitation)
                        || Double.isNaN(rootZoneSoilWetness)
                        || Double.isNaN(temperatureMax)
                        || Double.isNaN(temperatureMin)) {
                    logger.warn("Fecha {} de {} omitida por contener valores de relleno de NASA POWER.", date, producer.id());
                    continue;
                }

                result.add(new ClimateData(
                        producer.id(),
                        producer.name(),
                        producer.commodityType(),
                        date,
                        precipitation,
                        rootZoneSoilWetness,
                        temperatureMax,
                        temperatureMin
                ));
            } catch (Exception e) {
                logger.warn("Día {} de {} ignorado por error de parseo.", date, producer.id(), e);
            }
        }

        return result;
    }

    private boolean hasAllRequiredParameters(JsonObject parameterObject) {
        return parameterObject.has(PARAM_PRECTOTCORR)
                && parameterObject.has(PARAM_GWETROOT)
                && parameterObject.has(PARAM_T2M_MAX)
                && parameterObject.has(PARAM_T2M_MIN)
                && parameterObject.get(PARAM_PRECTOTCORR).isJsonObject()
                && parameterObject.get(PARAM_GWETROOT).isJsonObject()
                && parameterObject.get(PARAM_T2M_MAX).isJsonObject()
                && parameterObject.get(PARAM_T2M_MIN).isJsonObject();
    }

    private boolean isCompleteDate(
            String date,
            JsonObject prectotcorrSeries,
            JsonObject gwetrootSeries,
            JsonObject t2mMaxSeries,
            JsonObject t2mMinSeries
    ) {
        return prectotcorrSeries.has(date)
                && gwetrootSeries.has(date)
                && t2mMaxSeries.has(date)
                && t2mMinSeries.has(date)
                && !prectotcorrSeries.get(date).isJsonNull()
                && !gwetrootSeries.get(date).isJsonNull()
                && !t2mMaxSeries.get(date).isJsonNull()
                && !t2mMinSeries.get(date).isJsonNull();
    }

    private double readValue(JsonObject series, String date, Double fillValue) {
        double value = series.get(date).getAsDouble();

        if (fillValue != null && Double.compare(value, fillValue) == 0) {
            return Double.NaN;
        }

        return value;
    }
}