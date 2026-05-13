package org.ulpgc.dacd.businessunit.controller.datamart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteServingDatamartTest {

    @TempDir
    Path tempDir;

    private SqliteServingDatamart datamart;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("serving_test.db");
        datamart = new SqliteServingDatamart(url);
        datamart.initialize();
    }

    @Test
    void savesAndReadsBackSnapshot() {
        CommodityRiskSnapshot snapshot = new CommodityRiskSnapshot("WEAT", RiskLevel.HIGH, 89.5, "High risk reason");

        datamart.saveRiskSnapshot(snapshot);
        List<CommodityRiskSnapshot> result = datamart.findAllRiskSnapshots();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).commodity()).isEqualTo("WEAT");
        assertThat(result.get(0).riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.get(0).riskScore()).isEqualTo(89.5);
        assertThat(result.get(0).reason()).isEqualTo("High risk reason");
    }

    @Test
    void upsertUpdatesExistingSnapshot() {
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("WEAT", RiskLevel.LOW, 10.0, "Low risk"));
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("WEAT", RiskLevel.HIGH, 90.0, "High risk"));

        List<CommodityRiskSnapshot> result = datamart.findAllRiskSnapshots();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.get(0).riskScore()).isEqualTo(90.0);
    }

    @Test
    void findsByCommodity() {
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("WEAT", RiskLevel.HIGH, 89.5, "reason"));
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("CORN", RiskLevel.LOW, 10.0, "reason"));

        Optional<CommodityRiskSnapshot> result = datamart.findRiskSnapshotByCommodity("WEAT");

        assertThat(result).isPresent();
        assertThat(result.get().commodity()).isEqualTo("WEAT");
    }

    @Test
    void returnsEmptyWhenCommodityNotFound() {
        Optional<CommodityRiskSnapshot> result = datamart.findRiskSnapshotByCommodity("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void findsAllSnapshotsSortedByCommodity() {
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("WEAT", RiskLevel.HIGH, 89.5, "reason"));
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot("CORN", RiskLevel.LOW, 10.0, "reason"));

        List<CommodityRiskSnapshot> result = datamart.findAllRiskSnapshots();

        assertThat(result).extracting(CommodityRiskSnapshot::commodity)
                .containsExactly("CORN", "WEAT");
    }
}
