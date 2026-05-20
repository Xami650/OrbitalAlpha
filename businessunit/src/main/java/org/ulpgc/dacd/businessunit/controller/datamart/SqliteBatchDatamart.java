package org.ulpgc.dacd.businessunit.controller.datamart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteBatchDatamart implements BatchDatamart {

    private static final Logger logger = LoggerFactory.getLogger(SqliteBatchDatamart.class);

    private final String databaseUrl;

    public SqliteBatchDatamart(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @Override
    public void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS historical_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    topic TEXT NOT NULL,
                    source_system TEXT NOT NULL,
                    file_date TEXT NOT NULL,
                    raw_json TEXT NOT NULL
                )
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            logger.info("Batch datamart initialized");

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing batch datamart", e);
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM historical_events";

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
            logger.info("Batch datamart cleared");

        } catch (SQLException e) {
            throw new RuntimeException("Error clearing batch datamart", e);
        }
    }

    @Override
    public void saveHistoricalEvent(HistoricalEvent event) {
        String sql = """
                INSERT INTO historical_events (
                    topic,
                    source_system,
                    file_date,
                    raw_json
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, event.topic());
            statement.setString(2, event.sourceSystem());
            statement.setString(3, event.fileDate());
            statement.setString(4, event.rawJson());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving historical event", e);
        }
    }

    @Override
    public void saveHistoricalEvents(List<HistoricalEvent> events) {
        String sql = """
                INSERT INTO historical_events (
                    topic,
                    source_system,
                    file_date,
                    raw_json
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (HistoricalEvent event : events) {
                statement.setString(1, event.topic());
                statement.setString(2, event.sourceSystem());
                statement.setString(3, event.fileDate());
                statement.setString(4, event.rawJson());
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving historical events in batch", e);
        }
    }

    @Override
    public List<HistoricalEvent> findAllHistoricalEvents() {
        String sql = """
                SELECT topic, source_system, file_date, raw_json
                FROM historical_events
                ORDER BY file_date, topic, source_system
                """;

        List<HistoricalEvent> events = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                events.add(new HistoricalEvent(
                        resultSet.getString("topic"),
                        resultSet.getString("source_system"),
                        resultSet.getString("file_date"),
                        resultSet.getString("raw_json")
                ));
            }

            return events;

        } catch (SQLException e) {
            throw new RuntimeException("Error reading historical events from batch datamart", e);
        }
    }
}