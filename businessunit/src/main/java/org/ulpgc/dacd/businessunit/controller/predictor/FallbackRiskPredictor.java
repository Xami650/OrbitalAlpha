package org.ulpgc.dacd.businessunit.controller.predictor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;

public class FallbackRiskPredictor implements RiskPredictor {

    private static final Logger logger = LoggerFactory.getLogger(FallbackRiskPredictor.class);

    private final RiskPredictor primary;
    private final RiskPredictor fallback;
    private volatile boolean usingFallback = false;

    public FallbackRiskPredictor(RiskPredictor primary, RiskPredictor fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public CommodityRiskSnapshot predict(CommodityMetrics metrics) {
        try {
            CommodityRiskSnapshot result = primary.predict(metrics);
            usingFallback = false;
            return result;
        } catch (Exception e) {
            logger.warn("Primary predictor failed for {}, using heuristic fallback: {}", metrics.commodity(), e.getMessage());
            usingFallback = true;
            return fallback.predict(metrics);
        }
    }

    public boolean isUsingFallback() {
        return usingFallback;
    }
}
