package org.ulpgc.dacd;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

public class SqliteClimateStore implements ClimateStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteClimateStore.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:climate_data.db";
    private static final String ENV_KEY_DB_URL = "CLIMATE_DATABASE_URL";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS climate_daily_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                location_id TEXT NOT NULL,
                date TEXT NOT NULL,
                precipitation REAL,
                root_zone_soil_wetness REAL,
                temperature_max REAL,
                temperature_min REAL,
                captured_at TEXT NOT NULL,
                UNIQUE(location_id, date)
            );
            """;

    private static final String INSERT_SQL = """
            INSERT OR IGNORE INTO climate_daily_data
            (location_id, date, precipitation, root_zone_soil_wetness, temperature_max, temperature_min, captured_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final String dbUrl;

    public SqliteClimateStore() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.dbUrl = dotenv.get(ENV_KEY_DB_URL, DEFAULT_DB_URL);
        loadDriver();
    }

    public void initialize() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            statement.execute(CREATE_TABLE_SQL);
            logger.info("Base de datos climática inicializada.");

        } catch (SQLException e) {
            logger.error("Error inicializando la base de datos climática.", e);
        }
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
                pstmt.setString(1, data.locationId());
                pstmt.setString(2, data.date());
                pstmt.setDouble(3, data.precipitation());
                pstmt.setDouble(4, data.rootZoneSoilWetness());
                pstmt.setDouble(5, data.temperatureMax());
                pstmt.setDouble(6, data.temperatureMin());
                pstmt.setString(7, capturedAt);
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
        return DriverManager.getConnection(this.dbUrl);
    }
}
