package org.ulpgc.dacd.marketfeeder.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketFeederConfigLoaderTest {

    @Test
    void load_fileNotFound_throwsException() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("no-existe.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-existe.properties");
    }

    @Test
    void load_validConfig_returnsExpectedValues() {
        MarketFeederConfig config =
                new MarketFeederConfigLoader("marketfeeder-valid.properties").load();

        assertThat(config.apiKey()).isEqualTo("test-api-key");
        assertThat(config.symbols()).containsExactly("WEAT", "CORN", "SOYB");
        assertThat(config.intervalHours()).isEqualTo(24);
        assertThat(config.maxWeeks()).isEqualTo(520);
        assertThat(config.pauseMs()).isEqualTo(15000L);
        assertThat(config.brokerUrl()).isEqualTo("tcp://localhost:61616");
        assertThat(config.topic()).isEqualTo("CommodityPrice");
    }

    @Test
    void load_missingApiKey_throwsExceptionNamingProperty() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-missing-api-key.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("market.api.key");
    }

    @Test
    void load_blankApiKey_throwsExceptionNamingProperty() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-blank-api-key.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("market.api.key");
    }

    @Test
    void load_missingSymbols_throwsExceptionNamingProperty() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-missing-symbols.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("market.symbols");
    }

    @Test
    void load_missingBrokerUrl_throwsExceptionNamingProperty() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-missing-broker.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("broker.url");
    }

    @Test
    void load_missingTopic_throwsExceptionNamingProperty() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-missing-topic.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("market.topic");
    }

    @Test
    void load_missingOptionalNumericValues_usesDefaults() {
        MarketFeederConfig config =
                new MarketFeederConfigLoader("marketfeeder-default-values.properties").load();

        assertThat(config.intervalHours()).isEqualTo(24);
        assertThat(config.maxWeeks()).isEqualTo(520);
        assertThat(config.pauseMs()).isEqualTo(15000L);
    }

    @Test
    void load_symbolsWithDuplicatesAndSpaces_deduplicatesAndTrims() {
        MarketFeederConfig config =
                new MarketFeederConfigLoader("marketfeeder-dup-symbols.properties").load();

        assertThat(config.symbols())
                .containsExactly("WEAT", "CORN", "SOYB");
    }

    @Test
    void load_valuesWithSpaces_trimsValues() {
        MarketFeederConfig config =
                new MarketFeederConfigLoader("marketfeeder-trim-values.properties").load();

        assertThat(config.apiKey()).isEqualTo("test-api-key");
        assertThat(config.symbols()).isEqualTo(List.of("WEAT", "CORN"));
        assertThat(config.brokerUrl()).isEqualTo("tcp://localhost:61616");
        assertThat(config.topic()).isEqualTo("CommodityPrice");
    }

    @Test
    void load_invalidIntervalHours_throwsNumberFormatException() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-invalid-interval.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void load_invalidPauseMs_throwsNumberFormatException() {
        MarketFeederConfigLoader loader =
                new MarketFeederConfigLoader("marketfeeder-invalid-pause.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(NumberFormatException.class);
    }
}