package org.ulpgc.dacd.eventstorebuilder.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileEventStoreTest {

    @TempDir
    Path tempDir;

    FileEventStore store;

    @BeforeEach
    void setUp() {
        store = new FileEventStore(tempDir);
    }

    private String commodityPriceEvent(String symbol, String priceTimestamp) {
        return commodityPriceEventWithCaptureTime(
                symbol,
                priceTimestamp,
                "2026-04-29T22:56:26.952653Z",
                "AlphaVantage"
        );
    }

    private String commodityPriceEventWithCaptureTime(String symbol, String priceTimestamp, String ts) {
        return commodityPriceEventWithCaptureTime(
                symbol,
                priceTimestamp,
                ts,
                "AlphaVantage"
        );
    }

    private String commodityPriceEventWithCaptureTime(
            String symbol,
            String priceTimestamp,
            String ts,
            String sourceSystem
    ) {
        return ("{\"ts\":\"%s\",\"ss\":\"%s\","
                + "\"symbol\":\"%s\",\"priceTimestamp\":\"%s\","
                + "\"open\":23.28,\"high\":25.24,\"low\":23.25,\"close\":24.61,"
                + "\"adjustedClose\":24.61,\"volume\":4054912,\"dividendAmount\":0.0}")
                .formatted(ts, sourceSystem, symbol, priceTimestamp);
    }

    private String weatherEvent(String producerId, String date) {
        return weatherEventWithCaptureTime(
                producerId,
                date,
                "2026-04-29T22:56:24.448513Z",
                "weatherfeeder",
                "WHEAT"
        );
    }

    private String weatherEventWithProducer(String producerId) {
        return weatherEventWithCaptureTime(
                producerId,
                "20260426",
                "2026-04-29T22:56:24.448513Z",
                "weatherfeeder",
                "WHEAT"
        );
    }

    private String weatherEventWithCaptureTime(String producerId, String date, String ts) {
        return weatherEventWithCaptureTime(
                producerId,
                date,
                ts,
                "weatherfeeder",
                "WHEAT"
        );
    }

    private String weatherEventWithCaptureTime(
            String producerId,
            String date,
            String ts,
            String sourceSystem,
            String commodityType
    ) {
        return ("{\"ts\":\"%s\",\"ss\":\"%s\","
                + "\"producerId\":\"%s\",\"producerName\":\"Test Producer\","
                + "\"commodityType\":\"%s\",\"date\":\"%s\","
                + "\"precipitation\":0.0,\"rootZoneSoilWetness\":0.58,"
                + "\"temperatureMax\":21.33,\"temperatureMin\":7.9}")
                .formatted(ts, sourceSystem, producerId, commodityType, date);
    }

    private String simpleEvent(String ts, String ss) {
        return ("{\"ts\":\"%s\",\"ss\":\"%s\"}")
                .formatted(ts, ss);
    }

    private Path eventFile(String topic, String ss, String yyyyMMdd) {
        return tempDir.resolve(topic).resolve(ss).resolve(yyyyMMdd + ".events");
    }

    @Test
    void store_newCommodityPriceEvent_writesEventToExpectedFileUsingTsDate() throws IOException {
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-24T00:00:00Z"));

        assertThat(Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429")))
                .hasSize(1)
                .first().asString().contains("\"symbol\":\"WEAT\"");
    }

    @Test
    void store_newWeatherEvent_writesEventToExpectedFileUsingTsDate() throws IOException {
        store.store("Weather", weatherEvent("WHEAT_1", "20260426"));

        assertThat(Files.readAllLines(eventFile("Weather", "weatherfeeder", "20260429")))
                .hasSize(1)
                .first().asString().contains("\"producerId\":\"WHEAT_1\"");
    }

    @Test
    void store_multipleCommoditySymbolsOnSameCaptureDay_appendsAllEvents() throws IOException {
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("CORN", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("SOYB", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("JO", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("UNG", "2026-04-29T00:00:00Z"));

        List<String> lines = Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429"));

        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).contains("\"symbol\":\"WEAT\"");
        assertThat(lines.get(1)).contains("\"symbol\":\"CORN\"");
        assertThat(lines.get(2)).contains("\"symbol\":\"SOYB\"");
        assertThat(lines.get(3)).contains("\"symbol\":\"JO\"");
        assertThat(lines.get(4)).contains("\"symbol\":\"UNG\"");
    }

    @Test
    void store_sameCommodityWithDifferentPriceTimestamps_storesAllEvents() throws IOException {
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-24T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-17T00:00:00Z"));

        List<String> lines = Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429"));

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("\"priceTimestamp\":\"2026-04-29T00:00:00Z\"");
        assertThat(lines.get(1)).contains("\"priceTimestamp\":\"2026-04-24T00:00:00Z\"");
        assertThat(lines.get(2)).contains("\"priceTimestamp\":\"2026-04-17T00:00:00Z\"");
    }

    @Test
    void store_duplicateCommodityPriceBusinessEvent_isIgnoredEvenIfTsChanges() throws IOException {
        store.store("CommodityPrice", commodityPriceEventWithCaptureTime(
                "WEAT",
                "2026-04-29T00:00:00Z",
                "2026-04-29T22:56:26.952653Z"
        ));

        store.store("CommodityPrice", commodityPriceEventWithCaptureTime(
                "WEAT",
                "2026-04-29T00:00:00Z",
                "2026-04-29T23:10:00Z"
        ));

        List<String> lines = Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429"));

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst()).contains("\"symbol\":\"WEAT\"");
        assertThat(lines.getFirst()).contains("\"priceTimestamp\":\"2026-04-29T00:00:00Z\"");
    }

    @Test
    void store_sameCommodityAndSamePriceTimestampButDifferentSymbol_storesBothEvents() throws IOException {
        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z"));
        store.store("CommodityPrice", commodityPriceEvent("CORN", "2026-04-29T00:00:00Z"));

        List<String> lines = Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429"));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).contains("\"symbol\":\"WEAT\"");
        assertThat(lines.get(1)).contains("\"symbol\":\"CORN\"");
    }

    @Test
    void store_sameCommodityAndSamePriceTimestampFromDifferentSourceSystems_createsDifferentSourceFolders() {
        store.store("CommodityPrice", commodityPriceEventWithCaptureTime(
                "WEAT",
                "2026-04-29T00:00:00Z",
                "2026-04-29T22:56:26.952653Z",
                "AlphaVantage"
        ));

        store.store("CommodityPrice", commodityPriceEventWithCaptureTime(
                "WEAT",
                "2026-04-29T00:00:00Z",
                "2026-04-29T22:56:26.952653Z",
                "MarketBackup"
        ));

        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260429")).exists();
        assertThat(eventFile("CommodityPrice", "MarketBackup", "20260429")).exists();
    }

    @Test
    void store_duplicateWeatherBusinessEvent_isIgnoredEvenIfTsChanges() throws IOException {
        store.store("Weather", weatherEventWithCaptureTime(
                "WHEAT_1",
                "20260426",
                "2026-04-29T22:56:24.448513Z"
        ));

        store.store("Weather", weatherEventWithCaptureTime(
                "WHEAT_1",
                "20260426",
                "2026-04-29T23:10:00Z"
        ));

        List<String> lines = Files.readAllLines(eventFile("Weather", "weatherfeeder", "20260429"));

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst()).contains("\"producerId\":\"WHEAT_1\"");
        assertThat(lines.getFirst()).contains("\"date\":\"20260426\"");
    }

    @Test
    void store_weatherEventsWithDifferentProducerIds_areStoredAsDifferentEvents() throws IOException {
        store.store("Weather", weatherEventWithProducer("WHEAT_1"));
        store.store("Weather", weatherEventWithProducer("CORN_1"));
        store.store("Weather", weatherEventWithProducer("SOYB_1"));
        store.store("Weather", weatherEventWithProducer("COFFEE_1"));

        List<String> lines = Files.readAllLines(eventFile("Weather", "weatherfeeder", "20260429"));

        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).contains("\"producerId\":\"WHEAT_1\"");
        assertThat(lines.get(1)).contains("\"producerId\":\"CORN_1\"");
        assertThat(lines.get(2)).contains("\"producerId\":\"SOYB_1\"");
        assertThat(lines.get(3)).contains("\"producerId\":\"COFFEE_1\"");
    }

    @Test
    void store_sameWeatherProducerWithDifferentForecastDates_storesAllEventsInSameCaptureFile() throws IOException {
        store.store("Weather", weatherEvent("WHEAT_1", "20260426"));
        store.store("Weather", weatherEvent("WHEAT_1", "20260427"));
        store.store("Weather", weatherEvent("WHEAT_1", "20260428"));

        List<String> lines = Files.readAllLines(eventFile("Weather", "weatherfeeder", "20260429"));

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("\"date\":\"20260426\"");
        assertThat(lines.get(1)).contains("\"date\":\"20260427\"");
        assertThat(lines.get(2)).contains("\"date\":\"20260428\"");
    }

    @Test
    void store_weatherEventsWithDifferentCommodityTypes_areStoredAsDifferentEvents() throws IOException {
        store.store("Weather", weatherEventWithCaptureTime(
                "WHEAT_1",
                "20260426",
                "2026-04-29T22:56:24.448513Z",
                "weatherfeeder",
                "WHEAT"
        ));

        store.store("Weather", weatherEventWithCaptureTime(
                "CORN_1",
                "20260426",
                "2026-04-29T22:56:24.448513Z",
                "weatherfeeder",
                "CORN"
        ));

        store.store("Weather", weatherEventWithCaptureTime(
                "COFFEE_1",
                "20260426",
                "2026-04-29T22:56:24.448513Z",
                "weatherfeeder",
                "COFFEE"
        ));

        List<String> lines = Files.readAllLines(eventFile("Weather", "weatherfeeder", "20260429"));

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("\"commodityType\":\"WHEAT\"");
        assertThat(lines.get(1)).contains("\"commodityType\":\"CORN\"");
        assertThat(lines.get(2)).contains("\"commodityType\":\"COFFEE\"");
    }

    @Test
    void store_eventsWithDifferentCaptureDays_areStoredInDifferentFiles() {
        store.store("Weather", weatherEventWithCaptureTime(
                "WHEAT_1",
                "20260426",
                "2026-04-29T22:56:24.448513Z"
        ));

        store.store("Weather", weatherEventWithCaptureTime(
                "CORN_1",
                "20260426",
                "2026-04-30T00:01:00Z"
        ));

        store.store("Weather", weatherEventWithCaptureTime(
                "SOYB_1",
                "20260426",
                "2026-05-01T09:15:00Z"
        ));

        assertThat(eventFile("Weather", "weatherfeeder", "20260429")).exists();
        assertThat(eventFile("Weather", "weatherfeeder", "20260430")).exists();
        assertThat(eventFile("Weather", "weatherfeeder", "20260501")).exists();
    }

    @Test
    void store_sameSourceWithDifferentCaptureDates_createsDifferentDateFiles() {
        store.store("CommodityPrice", simpleEvent("2026-04-29T10:00:00Z", "AlphaVantage"));
        store.store("CommodityPrice", simpleEvent("2026-04-30T10:00:00Z", "AlphaVantage"));
        store.store("CommodityPrice", simpleEvent("2026-05-01T10:00:00Z", "AlphaVantage"));

        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260429")).exists();
        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260430")).exists();
        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260501")).exists();
    }

    @Test
    void store_sameEventWithDifferentTopics_createsDifferentTopicFolders() {
        String event = commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z");

        store.store("CommodityPrice", event);
        store.store("MarketBackup", event);
        store.store("AnalyticsInput", event);

        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260429")).exists();
        assertThat(eventFile("MarketBackup", "AlphaVantage", "20260429")).exists();
        assertThat(eventFile("AnalyticsInput", "AlphaVantage", "20260429")).exists();
    }

    @Test
    void store_sameTopicWithDifferentSourceSystems_createsDifferentSourceFolders() {
        store.store("CommodityPrice", simpleEvent("2026-04-29T10:00:00Z", "AlphaVantage"));
        store.store("CommodityPrice", simpleEvent("2026-04-29T10:00:00Z", "OtherSource"));
        store.store("CommodityPrice", simpleEvent("2026-04-29T10:00:00Z", "BackupSource"));

        assertThat(eventFile("CommodityPrice", "AlphaVantage", "20260429")).exists();
        assertThat(eventFile("CommodityPrice", "OtherSource", "20260429")).exists();
        assertThat(eventFile("CommodityPrice", "BackupSource", "20260429")).exists();
    }

    @Test
    void store_twoDifferentEvents_keepsBothLinesAndContents() throws IOException {
        String firstEvent = commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z");
        String secondEvent = commodityPriceEvent("UNG", "2026-04-29T00:00:00Z");

        store.store("CommodityPrice", firstEvent);
        store.store("CommodityPrice", secondEvent);

        List<String> lines = Files.readAllLines(eventFile("CommodityPrice", "AlphaVantage", "20260429"));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).contains("\"symbol\":\"WEAT\"");
        assertThat(lines.get(1)).contains("\"symbol\":\"UNG\"");
    }

    @Test
    void store_whenDirectoriesDoNotExist_createsDirectoriesAutomatically() {
        Path topicDirectory = tempDir.resolve("CommodityPrice");

        assertThat(topicDirectory).doesNotExist();

        store.store("CommodityPrice", commodityPriceEvent("WEAT", "2026-04-29T00:00:00Z"));

        assertThat(topicDirectory).exists();
        assertThat(tempDir.resolve("CommodityPrice").resolve("AlphaVantage")).exists();
    }

    @Test
    void store_eventMissingSsField_throwsException() {
        String event = "{\"ts\":\"2026-04-29T22:56:26.952653Z\","
                + "\"symbol\":\"WEAT\",\"priceTimestamp\":\"2026-04-29T00:00:00Z\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ss");
    }

    @Test
    void store_eventMissingTsField_throwsException() {
        String event = "{\"ss\":\"AlphaVantage\","
                + "\"symbol\":\"WEAT\",\"priceTimestamp\":\"2026-04-29T00:00:00Z\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ts");
    }

    @Test
    void store_eventWithBlankSsField_throwsException() {
        String event = "{\"ts\":\"2026-04-29T22:56:26.952653Z\",\"ss\":\"\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ss");
    }

    @Test
    void store_eventWithBlankTsField_throwsException() {
        String event = "{\"ts\":\"\",\"ss\":\"AlphaVantage\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ts");
    }

    @Test
    void store_eventWithNullSsField_throwsException() {
        String event = "{\"ts\":\"2026-04-29T22:56:26.952653Z\",\"ss\":null}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ss");
    }

    @Test
    void store_eventWithNullTsField_throwsException() {
        String event = "{\"ts\":null,\"ss\":\"AlphaVantage\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ts");
    }

    @Test
    void store_eventWithInvalidTsFormat_throwsException() {
        String event = "{\"ts\":\"20260429\",\"ss\":\"AlphaVantage\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void store_eventWithLocalDateInsteadOfInstant_throwsException() {
        String event = "{\"ts\":\"2026-04-29\",\"ss\":\"AlphaVantage\"}";

        assertThatThrownBy(() -> store.store("CommodityPrice", event))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void store_malformedJson_throwsException() {
        assertThatThrownBy(() -> store.store("CommodityPrice", "{ invalid json }"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void store_emptyJsonObject_throwsException() {
        assertThatThrownBy(() -> store.store("CommodityPrice", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required field");
    }
}