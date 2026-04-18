package org.ulpgc.dacd.marketfeeder.model.feeders;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AlphaVantageMarketFeeder implements MarketFeeder {

    private static final String URL =
            "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY_ADJUSTED&symbol=%s&apikey=%s";

    private final String apiKey;
    private final OkHttpClient client;

    public AlphaVantageMarketFeeder(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
    }

    @Override
    public String fetchWeeklySeriesRaw(String symbol) {
        String finalUrl = String.format(URL, symbol, apiKey);

        Request request = new Request.Builder()
                .url(finalUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP Error: " + response.code());
            }

            assert response.body() != null;
            return response.body().string();

        } catch (IOException e) {
            throw new RuntimeException("Error fetching data for " + symbol, e);
        }
    }
}