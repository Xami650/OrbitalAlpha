package org.ulpgc.dacd.businessunit.controller.serving;

import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.util.List;
import java.util.Optional;

public class CommodityRiskService {

    private final ServingDatamart servingDatamart;

    public CommodityRiskService(ServingDatamart servingDatamart) {
        this.servingDatamart = servingDatamart;
    }

    public void initializeDemoData() {
        if (!servingDatamart.findAllRiskSnapshots().isEmpty()) {
            return;
        }

        servingDatamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "WEAT",
                RiskLevel.HIGH,
                82.5,
                "Price increased and weather conditions are unfavorable",
                "heuristic-demo"
        ));

        servingDatamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "CORN",
                RiskLevel.MEDIUM,
                54.0,
                "Moderate price volatility",
                "heuristic-demo"
        ));

        servingDatamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "SOYB",
                RiskLevel.LOW,
                22.0,
                "Stable price and normal weather conditions",
                "heuristic-demo"
        ));

        servingDatamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "JO",
                RiskLevel.MEDIUM,
                61.5,
                "Weather risk detected in coffee region",
                "heuristic-demo"
        ));

        servingDatamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "UNG",
                RiskLevel.HIGH,
                77.0,
                "Strong market movement detected",
                "heuristic-demo"
        ));
    }

    public List<CommodityRiskSnapshot> getAllRisks() {
        return servingDatamart.findAllRiskSnapshots();
    }

    public Optional<CommodityRiskSnapshot> getRiskByCommodity(String commodity) {
        return servingDatamart.findRiskSnapshotByCommodity(commodity);
    }
}