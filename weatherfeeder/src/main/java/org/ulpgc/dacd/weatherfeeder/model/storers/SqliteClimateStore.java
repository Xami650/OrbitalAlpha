package org.ulpgc.dacd.weatherfeeder.model.storers;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.model.ClimateData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class SqliteClimateStore implements ClimateStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteClimateStore.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:climate_data.db";
    private static final String ENV_KEY_DB_URL = "CLIMATE_DATABASE_URL";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private static final String INSERT_SQL = """
            INSERT OR IGNORE INTO climate_daily_data
            (producer_id, producer_name, commodity_type, date, precipitation, root_zone_soil_wetness, temperature_max, temperature_min, captured_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final String dbUrl;

    public SqliteClimateStore() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.dbUrl = dotenv.get(ENV_KEY_DB_URL, DEFAULT_DB_URL);
        loadDriver();
    }

    @Override
    public void store(List<ClimateData> climateDataList) {
        if (climateDataList == null || climateDataList.isEmpty()) {
            logger.warn("No hay datos climáticos para almacenar.");
            return;
        }

        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(INSERT_SQL)) {

            connection.setAutoCommit(false);
            String capturedAt = Instant.now().toString();

            for (ClimateData data : climateDataList) {
                pstmt.setString(1, data.producerId());
                pstmt.setString(2, data.producerName());
                pstmt.setString(3, data.commodityType());
                pstmt.setString(4, data.date());
                pstmt.setDouble(5, data.precipitation());
                pstmt.setDouble(6, data.rootZoneSoilWetness());
                pstmt.setDouble(7, data.temperatureMax());
                pstmt.setDouble(8, data.temperatureMin());
                pstmt.setString(9, capturedAt);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            connection.commit();
            logger.info("Procesados {} registros climáticos para almacenamiento.", climateDataList.size());

        } catch (SQLException e) {
            logger.error("Error almacenando datos climáticos en SQLite.", e);
        }
    }

    private void loadDriver() {
        try {
            Class.forName(SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró el driver SQLite JDBC", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }
}