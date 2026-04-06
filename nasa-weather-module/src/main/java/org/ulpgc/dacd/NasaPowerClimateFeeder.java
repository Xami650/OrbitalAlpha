package org.ulpgc.dacd;

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
    private final Map<String, Location> locations;

    public NasaPowerClimateFeeder() {
        this.client = new OkHttpClient();
        this.locations = createLocations();
    }

    @Override
    public List<ClimateData> fetch(String locationId) {
        Location location = locations.get(locationId);

        if (location == null) {
            logger.error("Ubicación no reconocida: {}", locationId);
            return Collections.emptyList();
        }

        String endDate = LocalDate.now().format(API_DATE_FORMAT);
        String startDate = LocalDate.now().minusDays(DAYS_TO_FETCH - 1L).format(API_DATE_FORMAT);

        String url = buildUrl(location, startDate, endDate);
        Request request = buildRequest(url);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, locationId);
        } catch (IOException e) {
            logger.error("Fallo de conexión con NASA POWER para {}.", locationId, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error inesperado procesando {}.", locationId, e);
            return Collections.emptyList();
        }
    }

    private String buildUrl(Location location, String startDate, String endDate) {
        return String.format(
                API_URL_TEMPLATE,
                location.longitude(),
                location.latitude(),
                startDate,
                endDate
        );
    }

    private Request buildRequest(String url) {
        return new Request.Builder().url(url).get().build();
    }

    private List<ClimateData> handleResponse(Response response, String locationId) throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            logger.error("HTTP {} al consultar NASA POWER para {}.", response.code(), locationId);
            return Collections.emptyList();
        }

        String jsonResponse = response.body().string();
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

        logMessagesIfPresent(jsonObject, locationId);

        if (!jsonObject.has(KEY_PROPERTIES) || !jsonObject.get(KEY_PROPERTIES).isJsonObject()) {
            logger.error("La respuesta de NASA POWER para {} no contiene '{}'.", locationId, KEY_PROPERTIES);
            return Collections.emptyList();
        }

        JsonObject properties = jsonObject.getAsJsonObject(KEY_PROPERTIES);

        if (!properties.has(KEY_PARAMETER) || !properties.get(KEY_PARAMETER).isJsonObject()) {
            logger.error("La respuesta de NASA POWER para {} no contiene '{}'.", locationId, KEY_PARAMETER);
            return Collections.emptyList();
        }

        Double fillValue = extractFillValue(jsonObject);
        return parseParameters(locationId, properties.getAsJsonObject(KEY_PARAMETER), fillValue);
    }

    private void logMessagesIfPresent(JsonObject jsonObject, String locationId) {
        if (!jsonObject.has(KEY_MESSAGES) || !jsonObject.get(KEY_MESSAGES).isJsonArray()) {
            return;
        }

        JsonArray messages = jsonObject.getAsJsonArray(KEY_MESSAGES);

        if (!messages.isEmpty()) {
            logger.warn("NASA POWER devolvió mensajes para {}: {}", locationId, messages);
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

    private List<ClimateData> parseParameters(String locationId, JsonObject parameterObject, Double fillValue) {
        if (!hasAllRequiredParameters(parameterObject)) {
            logger.error("Faltan parámetros esperados en la respuesta de NASA POWER para {}.", locationId);
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
                logger.warn("Fecha {} incompleta para {}. Se omite.", date, locationId);
                continue;
            }

            try {
                double precipitation = readValue(prectotcorrSeries, date, fillValue);
                double rootZoneSoilWetness = readValue(gwetrootSeries, date, fillValue);
                double temperatureMax = readValue(t2mMaxSeries, date, fillValue);
                double temperatureMin = readValue(t2mMinSeries, date, fillValue);

                result.add(new ClimateData(
                        locationId,
                        date,
                        precipitation,
                        rootZoneSoilWetness,
                        temperatureMax,
                        temperatureMin
                ));
            } catch (Exception e) {
                logger.warn("Día {} de {} ignorado por error de parseo.", date, locationId, e);
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

    private Map<String, Location> createLocations() {
        Map<String, Location> map = new HashMap<>();
        map.put("LPA", new Location("28.1235", "-15.4363"));
        map.put("MAD", new Location("40.4168", "-3.7038"));
        map.put("BCN", new Location("41.3874", "2.1686"));
        map.put("SVQ", new Location("37.3891", "-5.9845"));
        map.put("VLC", new Location("39.4699", "-0.3763"));
        return map;
    }

    private record Location(String latitude, String longitude) {
    }
}
