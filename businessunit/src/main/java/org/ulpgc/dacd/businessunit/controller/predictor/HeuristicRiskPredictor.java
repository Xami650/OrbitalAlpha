package org.ulpgc.dacd.businessunit.controller.predictor;

import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

public class HeuristicRiskPredictor implements RiskPredictor {

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

        if (metrics.priceChangePercent() > 5) {
            score += 30;
        } else if (metrics.priceChangePercent() > 2) {
            score += 15;
        }

        if (metrics.precipitation() < 1) {
            score += 15;
        }

        if (metrics.rootZoneSoilWetness() > 0 && metrics.rootZoneSoilWetness() < 0.35) {
            score += 20;
        }

        if (metrics.temperatureMax() > 32) {
            score += 15;
        }

        if (metrics.temperatureMin() < 3) {
            score += 15;
        }

        if (metrics.priceVolatility() > 4) {
            score += 10;
        }

        if (metrics.priceTrend() > 3) {
            score += 10;
        }

        if (metrics.precipitationDelta() < -2) {
            score += 5;
        }

        if (metrics.soilWetnessDelta() < -0.15) {
            score += 5;
        }

        if (metrics.temperatureMaxDelta() > 5) {
            score += 5;
        }

        return Math.min(score, 100.0);
    }

    private RiskLevel calculateRiskLevel(double score) {
        if (score >= 80) return RiskLevel.HIGH;
        if (score >= 60) return RiskLevel.MEDIUM_HIGH;
        if (score >= 40) return RiskLevel.MEDIUM;
        if (score >= 20) return RiskLevel.LOW_MEDIUM;
        return RiskLevel.LOW;
    }

    private String buildReason(RiskLevel riskLevel, CommodityMetrics m) {
        List<String> triggers = new ArrayList<>();

        if (m.priceChangePercent() > 5) triggers.add("strong price increase");
        else if (m.priceChangePercent() > 2) triggers.add("moderate price increase");
        if (m.priceVolatility() > 4) triggers.add("high price volatility");
        if (m.priceTrend() > 3) triggers.add("sustained upward price trend");
        if (m.precipitation() < 1) triggers.add("low precipitation");
        if (m.precipitationDelta() < -2) triggers.add("precipitation below recent average");
        if (m.rootZoneSoilWetness() > 0 && m.rootZoneSoilWetness() < 0.35)
            triggers.add("low root-zone soil wetness");
        if (m.soilWetnessDelta() < -0.15) triggers.add("soil wetness below recent average");
        if (m.temperatureMax() > 32) triggers.add("high maximum temperature");
        if (m.temperatureMaxDelta() > 5) triggers.add("temperature above recent average");
        if (m.temperatureMin() < 3) triggers.add("low minimum temperature");

        if (triggers.isEmpty()) {
            return riskLevel + " risk: market and weather indicators remain stable.";
        }
        return riskLevel + " risk due to " + String.join(", ", triggers) + ".";
    }
}