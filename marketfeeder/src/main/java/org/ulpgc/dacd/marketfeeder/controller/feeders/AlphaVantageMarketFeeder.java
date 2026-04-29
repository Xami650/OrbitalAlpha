package org.ulpgc.dacd.marketfeeder.controller.feeders;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.ulpgc.dacd.marketfeeder.controller.exceptions.FeederConnectionException;
import org.ulpgc.dacd.marketfeeder.controller.feeders.parsers.MarketParser;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.io.IOException;
import java.util.List;

public class AlphaVantageMarketFeeder implements MarketFeeder {

    private static final String URL =
            "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY_ADJUSTED&symbol=%s&apikey=%s";

    private final String apiKey;
    private final OkHttpClient client;
    private final MarketParser parser;

    public AlphaVantageMarketFeeder(String apiKey, OkHttpClient client, MarketParser parser) {
        this.apiKey = apiKey;
        this.client = client;
        this.parser = parser;
    }

    @Override
    public List<MarketEvent> getMarketData(String symbol) {
        String rawResponse = fetchWeeklySeriesRaw(symbol);
        return parser.parse(symbol, rawResponse);
    }

    private String fetchWeeklySeriesRaw(String symbol) {
        String finalUrl = String.format(URL, symbol, apiKey);
        Request request = new Request.Builder().url(finalUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP Error: " + response.code());
            }
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            throw new FeederConnectionException("Error fetching data for " + symbol, e);
        }
    }
}