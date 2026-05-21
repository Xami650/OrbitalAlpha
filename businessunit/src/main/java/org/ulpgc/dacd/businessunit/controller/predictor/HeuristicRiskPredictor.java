package org.ulpgc.dacd.businessunit.controller.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeuristicRiskPredictor implements RiskPredictor {

    private static final Logger logger = LoggerFactory.getLogger(HeuristicRiskPredictor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String RULES_FILE_PATH = "../risk-rules.json";

    private JsonNode rules;

    public HeuristicRiskPredictor() {
        this.rules = loadRules();
    }

    private JsonNode loadRules() {
        String[] possiblePaths = {"risk-rules.json", "../risk-rules.json"};

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                logger.info("Loading risk rules from {}", file.getAbsolutePath());
                try {
                    return objectMapper.readTree(file);
                } catch (IOException e) {
                    logger.error("Failed to parse risk rules file at {}", path, e);
                    return null;
                }
            }
        }

        logger.warn("Risk rules file not found, using internal defaults.");
        return null;
    }

    @Override
    public CommodityRiskSnapshot predict(CommodityMetrics metrics) {
        double score = calculateRiskScore(metrics);
        RiskLevel riskLevel = calculateRiskLevel(score);

        return new CommodityRiskSnapshot(
                metrics.commodity(),
                riskLevel,
                score,
                buildReason(riskLevel, metrics)
        );
    }

    private double calculateRiskScore(CommodityMetrics metrics) {
        double score = 0.0;
        
        double strongThresh = getThreshold("priceChangeStrong", 5.0);
        double modThresh = getThreshold("priceChangeModerate", 2.0);
        
        if (metrics.priceChangePercent() > strongThresh) {
            score += getWeight("priceChangeStrong", 30.0);
        } else if (metrics.priceChangePercent() > modThresh) {
            score += getWeight("priceChangeModerate", 15.0);
        }

        if (metrics.precipitation() < getThreshold("lowPrecipitation", 1.0)) {
            score += getWeight("lowPrecipitation", 15.0);
        }

        if (metrics.rootZoneSoilWetness() > 0 && metrics.rootZoneSoilWetness() < getThreshold("lowSoilWetness", 0.35)) {
            score += getWeight("lowSoilWetness", 20.0);
        }

        if (metrics.temperatureMax() > getThreshold("highTemperatureMax", 32.0)) {
            score += getWeight("highTemperatureMax", 15.0);
        }

        if (metrics.temperatureMin() < getThreshold("lowTemperatureMin", 3.0)) {
            score += getWeight("lowTemperatureMin", 15.0);
        }

        if (metrics.priceVolatility() > getThreshold("highPriceVolatility", 4.0)) {
            score += getWeight("highPriceVolatility", 10.0);
        }

        if (metrics.priceTrend() > getThreshold("highPriceTrend", 3.0)) {
            score += getWeight("highPriceTrend", 10.0);
        }

        if (metrics.precipitationDelta() < getThreshold("lowPrecipitationDelta", -2.0)) {
            score += getWeight("lowPrecipitationDelta", 5.0);
        }

        if (metrics.soilWetnessDelta() < getThreshold("lowSoilWetnessDelta", -0.15)) {
            score += getWeight("lowSoilWetnessDelta", 5.0);
        }

        if (metrics.temperatureMaxDelta() > getThreshold("highTemperatureMaxDelta", 5.0)) {
            score += getWeight("highTemperatureMaxDelta", 5.0);
        }

        return Math.min(score, 100.0);
    }

    private double getThreshold(String key, double defaultValue) {
        if (rules != null && rules.has("thresholds") && rules.get("thresholds").has(key)) {
            return rules.get("thresholds").get(key).asDouble();
        }
        return defaultValue;
    }

    private double getWeight(String key, double defaultValue) {
        if (rules != null && rules.has("weights") && rules.get("weights").has(key)) {
            return rules.get("weights").get(key).asDouble();
        }
        return defaultValue;
    }

    private RiskLevel calculateRiskLevel(double score) {
        if (rules != null && rules.has("levels")) {
            for (JsonNode level : rules.get("levels")) {
                if (score >= level.get("minScore").asDouble()) {
                    return RiskLevel.valueOf(level.get("label").asText());
                }
            }
        }
        // Defaults if rules not loaded or levels missing
        if (score >= 80) return RiskLevel.HIGH;
        if (score >= 60) return RiskLevel.MEDIUM_HIGH;
        if (score >= 40) return RiskLevel.MEDIUM;
        if (score >= 20) return RiskLevel.LOW_MEDIUM;
        return RiskLevel.LOW;
    }

    private String buildReason(RiskLevel riskLevel, CommodityMetrics m) {
        List<String> triggers = new ArrayList<>();

        double strongThresh = getThreshold("priceChangeStrong", 5.0);
        double modThresh = getThreshold("priceChangeModerate", 2.0);

        if (m.priceChangePercent() > strongThresh) triggers.add("strong price increase");
        else if (m.priceChangePercent() > modThresh) triggers.add("moderate price increase");
        
        if (m.priceVolatility() > getThreshold("highPriceVolatility", 4.0)) triggers.add("high price volatility");
        if (m.priceTrend() > getThreshold("highPriceTrend", 3.0)) triggers.add("sustained upward price trend");
        
        if (m.precipitation() < getThreshold("lowPrecipitation", 1.0)) triggers.add("low precipitation");
        if (m.precipitationDelta() < getThreshold("lowPrecipitationDelta", -2.0)) triggers.add("precipitation below recent average");
        
        if (m.rootZoneSoilWetness() > 0 && m.rootZoneSoilWetness() < getThreshold("lowSoilWetness", 0.35))
            triggers.add("low root-zone soil wetness");
        
        if (m.soilWetnessDelta() < getThreshold("lowSoilWetnessDelta", -0.15)) triggers.add("soil wetness below recent average");
        if (m.temperatureMax() > getThreshold("highTemperatureMax", 32.0)) triggers.add("high maximum temperature");
        if (m.temperatureMaxDelta() > getThreshold("highTemperatureMaxDelta", 5.0)) triggers.add("temperature above recent average");
        if (m.temperatureMin() < getThreshold("lowTemperatureMin", 3.0)) triggers.add("low minimum temperature");

        if (triggers.isEmpty()) {
            return riskLevel + " risk: market and weather indicators remain stable.";
        }
        return riskLevel + " risk due to " + String.join(", ", triggers) + ".";
    }
}