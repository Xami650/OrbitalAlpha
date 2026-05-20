package org.ulpgc.dacd.marketfeeder.controller.feeder.parser;

import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.marketfeeder.model.MarketEvent;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlphaVantageMarketParserTest {

    @Test
    void parse_validWeeklyAdjustedTimeSeries_returnsMarketEvents() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        List<MarketEvent> events = parser.parse("WEAT", validResponseWithTwoWeeks());

        assertThat(events).hasSize(2);

        MarketEvent first = events.getFirst();

        assertThat(first.ss()).isEqualTo("AlphaVantage");
        assertThat(first.symbol()).isEqualTo("WEAT");
        assertThat(first.priceTimestamp()).isEqualTo(Instant.parse("2026-04-24T00:00:00Z"));
        assertThat(first.open()).isEqualTo(23.28);
        assertThat(first.high()).isEqualTo(25.24);
        assertThat(first.low()).isEqualTo(23.25);
        assertThat(first.close()).isEqualTo(24.61);
        assertThat(first.adjustedClose()).isEqualTo(24.61);
        assertThat(first.volume()).isEqualTo(4054912L);
        assertThat(first.dividendAmount()).isEqualTo(0.0);
        assertThat(first.ts()).isNotNull();
    }

    @Test
    void parse_respectsMaxWeeksLimit() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(1);

        List<MarketEvent> events = parser.parse("WEAT", validResponseWithTwoWeeks());

        assertThat(events).hasSize(1);
    }

    @Test
    void parse_errorMessage_throwsExceptionWithApiMessage() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        String rawResponse = """
                {
                  "Error Message": "Invalid API call."
                }
                """;

        assertThatThrownBy(() -> parser.parse("WEAT", rawResponse))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid API call.");
    }

    @Test
    void parse_noteMessage_throwsExceptionWithApiMessage() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        String rawResponse = """
                {
                  "Note": "API rate limit reached."
                }
                """;

        assertThatThrownBy(() -> parser.parse("WEAT", rawResponse))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API rate limit reached.");
    }

    @Test
    void parse_malformedJson_throwsException() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        assertThatThrownBy(() -> parser.parse("WEAT", "{ invalid json }"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_missingWeeklySeries_throwsException() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        String rawResponse = """
                {
                  "Meta Data": {
                    "1. Information": "Weekly Adjusted Prices"
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse("WEAT", rawResponse))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void parse_missingRequiredPriceField_throwsException() {
        AlphaVantageMarketParser parser = new AlphaVantageMarketParser(10);

        String rawResponse = """
                {
                  "Weekly Adjusted Time Series": {
                    "2026-04-24": {
                      "1. open": "23.28",
                      "2. high": "25.24",
                      "3. low": "23.25",
                      "4. close": "24.61",
                      "5. adjusted close": "24.61",
                      "6. volume": "4054912"
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse("WEAT", rawResponse))
                .isInstanceOf(RuntimeException.class);
    }

    private String validResponseWithTwoWeeks() {
        return """
                {
                  "Weekly Adjusted Time Series": {
                    "2026-04-24": {
                      "1. open": "23.28",
                      "2. high": "25.24",
                      "3. low": "23.25",
                      "4. close": "24.61",
                      "5. adjusted close": "24.61",
                      "6. volume": "4054912",
                      "7. dividend amount": "0.0000"
                    },
                    "2026-04-17": {
                      "1. open": "22.00",
                      "2. high": "23.00",
                      "3. low": "21.50",
                      "4. close": "22.75",
                      "5. adjusted close": "22.75",
                      "6. volume": "3054912",
                      "7. dividend amount": "0.0000"
                    }
                  }
                }
                """;
    }
}