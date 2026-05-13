package org.ulpgc.dacd.businessunit.model;

public record CommodityRiskSnapshot(
        String commodity,
        RiskLevel riskLevel,
        double riskScore,
        String reason
) {
}