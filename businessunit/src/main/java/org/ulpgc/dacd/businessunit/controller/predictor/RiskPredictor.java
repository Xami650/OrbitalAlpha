package org.ulpgc.dacd.businessunit.controller.predictor;

import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;

public interface RiskPredictor {

    CommodityRiskSnapshot predict(CommodityMetrics metrics);
}