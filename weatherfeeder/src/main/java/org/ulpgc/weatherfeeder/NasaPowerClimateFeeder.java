package org.ulpgc.weatherfeeder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NasaPowerClimateFeeder implements ClimateFeeder {

    private static final Logger logger = LoggerFactory.getLogger(NasaPowerClimateFeeder.class);

    private static final String API_URL_TEMPLATE =
            "https://power.larc.nasa.gov/api/temporal/daily/point" +
                    "?parameters=PRECTOTCORR,GWETROOT,T2M_MAX,T2M_MIN" +
                    "&community=AG" +
                    "&longitude=%s" +
                    "&latitude=%s" +
                    "&start=%s" +
                    "&end=%s" +
                    "&format=JSON";

    private static final int DAYS_TO_FETCH = 30;

    private static final String KEY_PROPERTIES = "properties";
    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_HEADER = "header";
    private static final String KEY_FILL_VALUE = "fill_value";
    private static final String KEY_MESSAGES = "messages";

    private static final String PARAM_PRECTOTCORR = "PRECTOTCORR";
    private static final String PARAM_GWETROOT = "GWETROOT";
    private static final String PARAM_T2M_MAX = "T2M_MAX";
    private static final String PARAM_T2M_MIN = "T2M_MIN";

    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final OkHttpClient client;
    private final Map<String, Producer> producers;

    public NasaPowerClimateFeeder() {
        this.client = new OkHttpClient();
        this.producers = createProducers();
    }

    @Override
    public List<ClimateData> fetch(String producerId) {
        Producer producer = producers.get(producerId);

        if (producer == null) {
            logger.error("Productor o región no reconocida: {}", producerId);
            return Collections.emptyList();
        }

        String endDate = LocalDate.now().format(API_DATE_FORMAT);
        String startDate = LocalDate.now().minusDays(DAYS_TO_FETCH - 1L).format(API_DATE_FORMAT);

        String url = buildUrl(producer, startDate, endDate);
        Request request = buildRequest(url);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, producer, producerId);
        } catch (IOException e) {
            logger.error("Fallo de conexión con NASA POWER para {}.", producerId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error inesperado procesando {}.", producerId, e);
            return Collections.emptyList();
        }
    }

    private String buildUrl(Producer producer, String startDate, String endDate) {
        return String.format(
                API_URL_TEMPLATE,
                producer.longitude(),
                producer.latitude(),
                startDate,
                endDate
        );
    }

    private Request buildRequest(String url) {
        return new Request.Builder().url(url).get().build();
    }

    private List<ClimateData> handleResponse(Response response, Producer producer, String producerId) throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            logger.error("HTTP {} al consultar NASA POWER para {}.", response.code(), producerId);
            return Collections.emptyList();
        }

        String jsonResponse = response.body().string();
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
        return parseParameters(producer, properties.getAsJsonObject(KEY_PARAMETER), fillValue);
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

    private List<ClimateData> parseParameters(Producer producer, JsonObject parameterObject, Double fillValue) {
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

    private Map<String, Producer> createProducers() {
        Map<String, Producer> map = new LinkedHashMap<>();

        // WHEAT
        map.put("WHEAT_1", new Producer("WHEAT_1", "Beauce - France", "WHEAT", 48.10, 1.75));
        map.put("WHEAT_2", new Producer("WHEAT_2", "Henan - China", "WHEAT", 34.75, 113.62));
        map.put("WHEAT_3", new Producer("WHEAT_3", "Punjab - India", "WHEAT", 31.15, 75.34));
        map.put("WHEAT_4", new Producer("WHEAT_4", "Krasnodar Krai - Russia", "WHEAT", 45.04, 38.98));
        map.put("WHEAT_5", new Producer("WHEAT_5", "Kansas - USA", "WHEAT", 38.50, -98.00));

        // CORN
        map.put("CORN_1", new Producer("CORN_1", "Iowa - USA", "CORN", 42.03, -93.58));
        map.put("CORN_2", new Producer("CORN_2", "Jilin - China", "CORN", 43.90, 125.32));
        map.put("CORN_3", new Producer("CORN_3", "Mato Grosso - Brazil", "CORN", -12.64, -55.42));
        map.put("CORN_4", new Producer("CORN_4", "Cordoba - Argentina", "CORN", -31.42, -64.19));
        map.put("CORN_5", new Producer("CORN_5", "Baragan Plain - Romania", "CORN", 44.50, 27.50));

        // SOY BEANS
        map.put("SOY_1", new Producer("SOY_1", "Mato Grosso - Brazil", "SOY_BEANS", -12.64, -55.42));
        map.put("SOY_2", new Producer("SOY_2", "Illinois - USA", "SOY_BEANS", 40.00, -89.20));
        map.put("SOY_3", new Producer("SOY_3", "Cordoba - Argentina", "SOY_BEANS", -31.42, -64.19));
        map.put("SOY_4", new Producer("SOY_4", "Heilongjiang - China", "SOY_BEANS", 47.86, 127.76));
        map.put("SOY_5", new Producer("SOY_5", "Alto Parana - Paraguay", "SOY_BEANS", -25.45, -54.90));

        // COFFEE
        map.put("COFFEE_1", new Producer("COFFEE_1", "Minas Gerais - Brazil", "COFFEE", -18.51, -44.56));
        map.put("COFFEE_2", new Producer("COFFEE_2", "Dak Lak - Vietnam", "COFFEE", 12.71, 108.24));
        map.put("COFFEE_3", new Producer("COFFEE_3", "Antioquia - Colombia", "COFFEE", 6.55, -75.57));
        map.put("COFFEE_4", new Producer("COFFEE_4", "Lampung - Indonesia", "COFFEE", -4.56, 105.41));
        map.put("COFFEE_5", new Producer("COFFEE_5", "Oromia - Ethiopia", "COFFEE", 7.55, 40.63));

        // NATURAL GAS
        map.put("NATGAS_1", new Producer("NATGAS_1", "Texas - USA", "NATURAL_GAS", 31.00, -99.00));
        map.put("NATGAS_2", new Producer("NATGAS_2", "Pennsylvania - USA", "NATURAL_GAS", 41.20, -77.19));
        map.put("NATGAS_3", new Producer("NATGAS_3", "Louisiana - USA", "NATURAL_GAS", 31.00, -92.00));
        map.put("NATGAS_4", new Producer("NATGAS_4", "West Virginia - USA", "NATURAL_GAS", 38.60, -80.45));
        map.put("NATGAS_5", new Producer("NATGAS_5", "Oklahoma - USA", "NATURAL_GAS", 35.59, -97.49));

        return map;
    }

    private record Producer(
            String id,
            String name,
            String commodityType,
            double latitude,
            double longitude
    ) {}
}