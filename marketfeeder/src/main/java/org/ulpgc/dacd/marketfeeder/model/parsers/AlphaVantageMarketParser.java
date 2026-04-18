package org.ulpgc.dacd.marketfeeder.model.parsers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlphaVantageMarketParser implements MarketParser {

    private final int maxWeeks;

    public AlphaVantageMarketParser(int maxWeeks) {
        this.maxWeeks = maxWeeks;
    }

    @Override
    public List<CommoditiesInfo> parse(String symbol, String rawResponse) {

        List<CommoditiesInfo> result = new ArrayList<>();

        JsonObject root = JsonParser.parseString(rawResponse).getAsJsonObject();

        if (root.has("Error Message")) {
            throw new RuntimeException(root.get("Error Message").getAsString());
        }

        if (root.has("Note")) {
            throw new RuntimeException(root.get("Note").getAsString());
        }

        JsonObject series = root.getAsJsonObject("Weekly Adjusted Time Series");

        int count = 0;

        for (Map.Entry<String, JsonElement> entry : series.entrySet()) {

            if (count >= maxWeeks) break;

            String date = entry.getKey();
            JsonObject values = entry.getValue().getAsJsonObject();

            CommoditiesInfo data = new CommoditiesInfo(
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

            result.add(data);
            count++;
        }

        return result;
    }
}