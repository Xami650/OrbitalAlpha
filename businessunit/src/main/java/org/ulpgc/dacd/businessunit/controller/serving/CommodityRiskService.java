package org.ulpgc.dacd.businessunit.controller.serving;

import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;

import java.util.List;
import java.util.Optional;

public class CommodityRiskService {

    private final ServingDatamart servingDatamart;

    public CommodityRiskService(ServingDatamart servingDatamart) {
        this.servingDatamart = servingDatamart;
    }

    public List<CommodityRiskSnapshot> getAllRisks() {
        return servingDatamart.findAllRiskSnapshots();
    }

    public Optional<CommodityRiskSnapshot> getRiskByCommodity(String commodity) {
        return servingDatamart.findRiskSnapshotByCommodity(commodity);
    }
}