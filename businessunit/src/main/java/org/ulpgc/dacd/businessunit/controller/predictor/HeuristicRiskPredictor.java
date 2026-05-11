package org.ulpgc.dacd.businessunit.controller.predictor;

import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

public class HeuristicRiskPredictor implements RiskPredictor {

    @Override
    public CommodityRiskSnapshot predict(CommodityMetrics metrics) {
        double score = calculateRiskScore(metrics);
        RiskLevel riskLevel = calculateRiskLevel(score);

        return new CommodityRiskSnapshot(
                metrics.commodity(),
                riskLevel,
                score,
                buildReason(riskLevel)
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

        return Math.min(score, 100.0);
    }

    private RiskLevel calculateRiskLevel(double score) {
        if (score >= 70) {
            return RiskLevel.HIGH;
        }

        if (score >= 40) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    private String buildReason(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> "High risk detected from price movement and/or adverse weather conditions";
            case MEDIUM -> "Moderate risk detected from market or weather indicators";
            case LOW -> "Low risk: price and weather indicators remain stable";
        };
    }
}