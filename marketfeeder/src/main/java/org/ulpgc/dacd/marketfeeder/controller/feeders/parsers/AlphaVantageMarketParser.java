package org.ulpgc.dacd.marketfeeder.controller.feeders.parsers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;


public class AlphaVantageMarketParser implements MarketParser {

    private final int maxWeeks;
    private static final String SOURCE_SYSTEM = "AlphaVantage";

    public AlphaVantageMarketParser(int maxWeeks) {
        this.maxWeeks = maxWeeks;
    }

    @Override
    public List<MarketEvent> parse(String symbol, String rawResponse) {

        JsonObject root = JsonParser.parseString(rawResponse).getAsJsonObject();

        if (root.has("Error Message")) {
            throw new RuntimeException(root.get("Error Message").getAsString());
        }
        if (root.has("Note")) {
            throw new RuntimeException(root.get("Note").getAsString());
        }

        JsonObject series = root.getAsJsonObject("Weekly Adjusted Time Series");

        return series.entrySet().stream()
                .limit(maxWeeks)
                .map(entry -> {
                    String date = entry.getKey();
                    JsonObject values = entry.getValue().getAsJsonObject();

                    return new MarketEvent(
                            Instant.now(),
                            SOURCE_SYSTEM,
                            symbol,
                            LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant(),
                            values.get("1. open").getAsDouble(),
                            values.get("2. high").getAsDouble(),
                            values.get("3. low").getAsDouble(),
                            values.get("4. close").getAsDouble(),
                            values.get("5. adjusted close").getAsDouble(),
                            values.get("6. volume").getAsLong(),
                            values.get("7. dividend amount").getAsDouble()
                    );
                })
                .toList();
    }
}