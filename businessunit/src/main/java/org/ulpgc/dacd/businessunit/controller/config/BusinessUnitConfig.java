package org.ulpgc.dacd.businessunit.controller.config;

import java.nio.file.Path;
import java.util.List;

public record BusinessUnitConfig(
        String brokerUrl,
        String clientId,
        List<String> topics,
        Path eventStorePath,
        String batchDatamartUrl,
        String servingDatamartUrl,
        int apiPort,
        String mlApiUrl,
        int debounceDelaySeconds
) {
}