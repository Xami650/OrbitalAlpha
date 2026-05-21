package org.ulpgc.dacd.marketfeeder.controller.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.controller.exceptions.FeederConnectionException;
import org.ulpgc.dacd.marketfeeder.controller.feeder.parser.MarketParser;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.io.IOException;
import java.util.List;

public class AlphaVantageMarketFeeder implements MarketFeeder {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageMarketFeeder.class);

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
                logger.error("HTTP error {} fetching data for {}", response.code(), symbol);
                throw new RuntimeException("HTTP Error: " + response.code());
            }
            if (response.body() == null) {
                throw new FeederConnectionException("Empty response body for " + symbol);
            }
            return response.body().string();
        } catch (IOException e) {
            logger.error("Connection error fetching data for {}", symbol, e);
            throw new FeederConnectionException("Error fetching data for " + symbol, e);
        }
    }
}