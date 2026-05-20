package org.ulpgc.dacd.eventstorebuilder.config;

import java.nio.file.Path;
import java.util.List;

public record EventStoreBuilderConfig(
        String brokerUrl,
        String clientId,
        Path eventStorePath,
        List<String> topics
) {
}