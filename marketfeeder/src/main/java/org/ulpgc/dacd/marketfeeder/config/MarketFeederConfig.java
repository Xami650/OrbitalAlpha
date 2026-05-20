package org.ulpgc.dacd.marketfeeder.config;

import java.util.List;

public record MarketFeederConfig(
        String apiKey,
        List<String> symbols,
        int intervalHours,
        int maxWeeks,
        long pauseMs,
        String brokerUrl,
        String topic
) {
}