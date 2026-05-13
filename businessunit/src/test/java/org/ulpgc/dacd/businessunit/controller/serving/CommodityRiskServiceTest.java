package org.ulpgc.dacd.businessunit.controller.serving;

import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CommodityRiskServiceTest {

    private static final CommodityRiskSnapshot WEAT = new CommodityRiskSnapshot("WEAT", RiskLevel.HIGH, 89.5, "High risk");
    private static final CommodityRiskSnapshot CORN = new CommodityRiskSnapshot("CORN", RiskLevel.LOW, 15.0, "Low risk");

    @Test
    void getAllRisksDelegatesToDatamart() {
        CommodityRiskService service = new CommodityRiskService(new FakeServingDatamart(List.of(WEAT, CORN)));

        List<CommodityRiskSnapshot> result = service.getAllRisks();

        assertThat(result).containsExactly(WEAT, CORN);
    }

    @Test
    void getRiskByCommodityReturnsPresentWhenFound() {
        CommodityRiskService service = new CommodityRiskService(new FakeServingDatamart(List.of(WEAT)));

        Optional<CommodityRiskSnapshot> result = service.getRiskByCommodity("WEAT");

        assertThat(result).isPresent();
        assertThat(result.get().commodity()).isEqualTo("WEAT");
    }

    @Test
    void getRiskByCommodityReturnsEmptyWhenNotFound() {
        CommodityRiskService service = new CommodityRiskService(new FakeServingDatamart(List.of()));

        Optional<CommodityRiskSnapshot> result = service.getRiskByCommodity("UNKNOWN");

        assertThat(result).isEmpty();
    }

    private static class FakeServingDatamart implements ServingDatamart {

        private final List<CommodityRiskSnapshot> snapshots;

        FakeServingDatamart(List<CommodityRiskSnapshot> snapshots) {
            this.snapshots = snapshots;
        }

        @Override
        public void initialize() {}

        @Override
        public void saveRiskSnapshot(CommodityRiskSnapshot snapshot) {}

        @Override
        public List<CommodityRiskSnapshot> findAllRiskSnapshots() {
            return snapshots;
        }

        @Override
        public Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity) {
            return snapshots.stream()
                    .filter(s -> s.commodity().equalsIgnoreCase(commodity))
                    .findFirst();
        }
    }
}
