package org.ulpgc.dacd.businessunit.controller.processor;

import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CommodityPriceEventProcessorTest {

    private final CommodityPriceEventProcessor processor = new CommodityPriceEventProcessor();

    @Test
    void computesPriceChangePercentBetweenLatestAndPrevious() {
        List<HistoricalEvent> events = List.of(
                priceEvent("WEAT", "2026-04-24T00:00:00Z", 20.0),
                priceEvent("WEAT", "2026-04-29T00:00:00Z", 22.0)
        );

        Map<String, CommodityPriceEventProcessor.PriceMetrics> result = processor.process(events);

        assertThat(result).containsKey("WEAT");
        assertThat(result.get("WEAT").latestClosePrice()).isEqualTo(22.0);
        assertThat(result.get("WEAT").previousClosePrice()).isEqualTo(20.0);
        assertThat(result.get("WEAT").priceChangePercent()).isCloseTo(10.0, within(0.001));
    }

    @Test
    void usesLatestTimestampAsCurrentPrice() {
        List<HistoricalEvent> events = List.of(
                priceEvent("CORN", "2026-04-29T00:00:00Z", 18.0),
                priceEvent("CORN", "2026-04-24T00:00:00Z", 17.0),
                priceEvent("CORN", "2026-04-17T00:00:00Z", 16.0)
        );

        Map<String, CommodityPriceEventProcessor.PriceMetrics> result = processor.process(events);

        assertThat(result.get("CORN").latestClosePrice()).isEqualTo(18.0);
        assertThat(result.get("CORN").previousClosePrice()).isEqualTo(17.0);
    }

    @Test
    void ignoresNonCommodityPriceEvents() {
        List<HistoricalEvent> events = List.of(
                weatherEvent("WHEAT"),
                priceEvent("WEAT", "2026-04-29T00:00:00Z", 22.0)
        );

        Map<String, CommodityPriceEventProcessor.PriceMetrics> result = processor.process(events);

        assertThat(result).containsOnlyKeys("WEAT");
    }

    @Test
    void handlesMultipleCommodities() {
        List<HistoricalEvent> events = List.of(
                priceEvent("WEAT", "2026-04-24T00:00:00Z", 20.0),
                priceEvent("WEAT", "2026-04-29T00:00:00Z", 21.0),
                priceEvent("CORN", "2026-04-24T00:00:00Z", 15.0),
                priceEvent("CORN", "2026-04-29T00:00:00Z", 16.0)
        );

        Map<String, CommodityPriceEventProcessor.PriceMetrics> result = processor.process(events);

        assertThat(result).containsKeys("WEAT", "CORN");
    }

    @Test
    void returnsEmptyMapWhenNoEvents() {
        assertThat(processor.process(List.of())).isEmpty();
    }

    private HistoricalEvent priceEvent(String symbol, String priceTimestamp, double close) {
        String json = String.format(
                "{\"symbol\":\"%s\",\"priceTimestamp\":\"%s\",\"close\":%s}",
                symbol, priceTimestamp, close
        );
        return new HistoricalEvent("CommodityPrice", "AlphaVantage", "20260429", json);
    }

    private HistoricalEvent weatherEvent(String commodityType) {
        String json = String.format("{\"commodityType\":\"%s\",\"date\":\"20260429\"}", commodityType);
        return new HistoricalEvent("Weather", "weatherfeeder", "20260429", json);
    }
}
