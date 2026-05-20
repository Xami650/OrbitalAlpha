package org.ulpgc.dacd.businessunit.controller.datamart;

import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;

import java.util.List;
import java.util.Optional;

public interface ServingDatamart {

    void initialize();

    void saveRiskSnapshot(CommodityRiskSnapshot snapshot);

    List<CommodityRiskSnapshot> findAllRiskSnapshots();

    Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity);
}