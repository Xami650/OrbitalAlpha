package org.ulpgc.dacd.businessunit.controller.datamart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteServingDatamart implements ServingDatamart {

    private static final Logger logger = LoggerFactory.getLogger(SqliteServingDatamart.class);

    private final String databaseUrl;

    public SqliteServingDatamart(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @Override
    public void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS commodity_risk_snapshots (
                    commodity TEXT PRIMARY KEY,
                    risk_level TEXT NOT NULL,
                    risk_score REAL NOT NULL,
                    reason TEXT NOT NULL
                )
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            logger.info("Serving datamart initialized");

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing serving datamart", e);
        }
    }

    @Override
    public void saveRiskSnapshot(CommodityRiskSnapshot snapshot) {
        String sql = """
                INSERT INTO commodity_risk_snapshots (
                    commodity,
                    risk_level,
                    risk_score,
                    reason
                )
                VALUES (?, ?, ?, ?)
                ON CONFLICT(commodity) DO UPDATE SET
                    risk_level = excluded.risk_level,
                    risk_score = excluded.risk_score,
                    reason = excluded.reason
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, snapshot.commodity());
            statement.setString(2, snapshot.riskLevel().name());
            statement.setDouble(3, snapshot.riskScore());
            statement.setString(4, snapshot.reason());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving risk snapshot for " + snapshot.commodity(), e);
        }
    }

    @Override
    public List<CommodityRiskSnapshot> findAllRiskSnapshots() {
        String sql = """
                SELECT commodity, risk_level, risk_score, reason
                FROM commodity_risk_snapshots
                ORDER BY commodity
                """;

        List<CommodityRiskSnapshot> snapshots = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                snapshots.add(toSnapshot(resultSet));
            }

            return snapshots;

        } catch (SQLException e) {
            throw new RuntimeException("Error reading risk snapshots", e);
        }
    }

    @Override
    public Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity) {
        String sql = """
                SELECT commodity, risk_level, risk_score, reason
                FROM commodity_risk_snapshots
                WHERE commodity = ?
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, commodity.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(toSnapshot(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error reading risk snapshot for " + commodity, e);
        }
    }

    private CommodityRiskSnapshot toSnapshot(ResultSet resultSet) throws SQLException {
        return new CommodityRiskSnapshot(
                resultSet.getString("commodity"),
                RiskLevel.valueOf(resultSet.getString("risk_level")),
                resultSet.getDouble("risk_score"),
                resultSet.getString("reason")
        );
    }
}