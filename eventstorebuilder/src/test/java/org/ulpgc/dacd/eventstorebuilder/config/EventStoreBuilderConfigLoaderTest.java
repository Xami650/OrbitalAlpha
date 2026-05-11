package org.ulpgc.dacd.eventstorebuilder.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventStoreBuilderConfigLoaderTest {

    @Test
    void load_fileNotFound_throwsException() {
        EventStoreBuilderConfigLoader loader =
                new EventStoreBuilderConfigLoader("no-existe.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-existe.properties");
    }

    @Test
    void load_missingBrokerUrl_throwsExceptionNamingProperty() {
        EventStoreBuilderConfigLoader loader =
                new EventStoreBuilderConfigLoader("eventstorebuilder-missing-broker.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("broker.url");
    }

    @Test
    void load_blankClientId_throwsExceptionNamingProperty() {
        EventStoreBuilderConfigLoader loader =
                new EventStoreBuilderConfigLoader("eventstorebuilder-blank-clientid.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client.id");
    }

    @Test
    void load_missingEventStorePath_throwsExceptionNamingProperty() {
        EventStoreBuilderConfigLoader loader =
                new EventStoreBuilderConfigLoader("eventstorebuilder-missing-path.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event.store.path");
    }

    @Test
    void load_missingTopics_throwsExceptionNamingProperty() {
        EventStoreBuilderConfigLoader loader =
                new EventStoreBuilderConfigLoader("eventstorebuilder-missing-topics.properties");

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("topics");
    }

    @Test
    void load_validConfig_returnsExpectedValues() {
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader("eventstorebuilder-valid.properties").load();

        assertThat(config.brokerUrl()).isEqualTo("tcp://localhost:61616");
        assertThat(config.clientId()).isEqualTo("test-client");
        assertThat(config.eventStorePath()).isEqualTo(Path.of("/tmp/events"));
        assertThat(config.topics()).containsExactly("topicA", "topicB");
    }

    @Test
    void load_topicsWithDuplicatesAndSpaces_deduplicatesAndTrims() {
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader("eventstorebuilder-dup-topics.properties").load();

        assertThat(config.topics())
                .containsExactly("topicA", "topicB");
    }

    @Test
    void load_topicsWithExtraCommas_removesBlankTopics() {
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader("eventstorebuilder-extra-commas.properties").load();

        assertThat(config.topics())
                .containsExactly("CommodityPrice", "Weather");
    }

    @Test
    void load_valuesWithSpaces_trimsValues() {
        EventStoreBuilderConfig config =
                new EventStoreBuilderConfigLoader("eventstorebuilder-trim-values.properties").load();

        assertThat(config.brokerUrl()).isEqualTo("tcp://localhost:61616");
        assertThat(config.clientId()).isEqualTo("test-client");
        assertThat(config.eventStorePath()).isEqualTo(Path.of("/tmp/events"));
        assertThat(config.topics()).containsExactly("CommodityPrice", "Weather");
    }
}