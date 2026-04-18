package org.ulpgc.dacd.weatherfeeder.model.storers;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteClimateDatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SqliteClimateDatabaseInitializer.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:climate_data.db";
    private static final String ENV_KEY_DB_URL = "CLIMATE_DATABASE_URL";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS climate_daily_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                producer_id TEXT NOT NULL,
                producer_name TEXT NOT NULL,
                commodity_type TEXT NOT NULL,
                date TEXT NOT NULL,
                precipitation REAL,
                root_zone_soil_wetness REAL,
                temperature_max REAL,
                temperature_min REAL,
                captured_at TEXT NOT NULL,
                UNIQUE(producer_id, date)
            );
            """;

    private final String dbUrl;

    public SqliteClimateDatabaseInitializer() {
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