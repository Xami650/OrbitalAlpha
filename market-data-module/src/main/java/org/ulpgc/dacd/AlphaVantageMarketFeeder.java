package org.ulpgc.dacd;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlphaVantageMarketFeeder implements MarketFeeder {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageMarketFeeder.class);

    private static final String API_URL_TEMPLATE =
            "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY_ADJUSTED&symbol=%s&apikey=%s";

    private static final String ENV_API_KEY = "MARKET_API_KEY";
    private static final int MAX_WEEKS_TO_FETCH = 520;

    private static final String KEY_NOTE = "Note";
    private static final String KEY_ERROR_MESSAGE = "Error Message";
    private static final String KEY_TIME_SERIES = "Weekly Adjusted Time Series";

    private static final String KEY_OPEN = "1. open";
    private static final String KEY_HIGH = "2. high";
    private static final String KEY_LOW = "3. low";
    private static final String KEY_CLOSE = "4. close";
    private static final String KEY_ADJUSTED_CLOSE = "5. adjusted close";
    private static final String KEY_VOLUME = "6. volume";
    private static final String KEY_DIVIDEND_AMOUNT = "7. dividend amount";

    private final OkHttpClient client;
    private final String apiKey;

    public AlphaVantageMarketFeeder() {
        this.client = new OkHttpClient();
        this.apiKey = loadApiKey();
    }

    @Override
    public List<MarketData> fetch(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            logger.warn("Símbolo inválido.");
            return Collections.emptyList();
        }

        String url = buildUrl(symbol);
        Request request = buildRequest(url);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, symbol);
        } catch (IOException e) {
            logger.error("Fallo de conexión con Alpha Vantage para {}.", symbol, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error inesperado procesando {}.", symbol, e);
            return Collections.emptyList();
        }
    }

    private String loadApiKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String key = dotenv.get(ENV_API_KEY);

        if (key == null || key.isBlank()) {
            throw new IllegalStateException("No se encontró MARKET_API_KEY en .env");
        }

        return key;
    }

    private String buildUrl(String symbol) {
        return String.format(API_URL_TEMPLATE, symbol, this.apiKey);
    }

    private Request buildRequest(String url) {
        return new Request.Builder().url(url).get().build();
    }

    private List<MarketData> handleResponse(Response response, String symbol) throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            logger.error("HTTP {} al consultar {}.", response.code(), symbol);
            return Collections.emptyList();
        }

        String jsonResponse = response.body().string();
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

        if (jsonObject.has(KEY_NOTE)) {
            logger.warn("Aviso/límite de Alpha Vantage para {}: {}", symbol, jsonObject.get(KEY_NOTE).getAsString());
            return Collections.emptyList();
        }

        if (jsonObject.has(KEY_ERROR_MESSAGE)) {
            logger.error("Error de Alpha Vantage para {}: {}", symbol, jsonObject.get(KEY_ERROR_MESSAGE).getAsString());
            return Collections.emptyList();
        }

        if (!jsonObject.has(KEY_TIME_SERIES)) {
            logger.warn("La respuesta para {} no contiene '{}'.", symbol, KEY_TIME_SERIES);
            return Collections.emptyList();
        }

        return parseTimeSeries(symbol, jsonObject.getAsJsonObject(KEY_TIME_SERIES));
    }

    private List<MarketData> parseTimeSeries(String symbol, JsonObject timeSeries) {
        List<MarketData> result = new ArrayList<>();
        List<String> dates = new ArrayList<>(timeSeries.keySet());
        dates.sort(Collections.reverseOrder());

        int limit = Math.min(MAX_WEEKS_TO_FETCH, dates.size());

        for (int i = 0; i < limit; i++) {
            String weekDate = dates.get(i);
            JsonObject week = timeSeries.getAsJsonObject(weekDate);

            try {
                result.add(new MarketData(
                        symbol,
                        weekDate,
                        week.get(KEY_OPEN).getAsDouble(),
                        week.get(KEY_HIGH).getAsDouble(),
                        week.get(KEY_LOW).getAsDouble(),
                        week.get(KEY_CLOSE).getAsDouble(),
                        week.get(KEY_ADJUSTED_CLOSE).getAsDouble(),
                        week.get(KEY_VOLUME).getAsLong(),
                        week.get(KEY_DIVIDEND_AMOUNT).getAsDouble()
                ));
            } catch (Exception e) {
                logger.warn("Semana {} de {} ignorada por error de parseo.", weekDate, symbol, e);
            }
        }

        return result;
    }
}