package org.ulpgc.dacd.businessunit.controller.predictor;

import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;

/** Predicts the supply-chain risk level for a commodity based on market and weather metrics. */
public interface RiskPredictor {

    CommodityRiskSnapshot predict(CommodityMetrics metrics);
}