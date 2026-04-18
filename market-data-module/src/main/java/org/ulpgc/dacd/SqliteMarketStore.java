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

public class SqliteMarketStore implements MarketStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteMarketStore.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:mercado_datos.db";
    private static final String ENV_KEY_DB_URL = "MARKET_DATABASE_URL";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS market_commodity_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                week_date TEXT NOT NULL,
                open REAL,
                high REAL,
                low REAL,
                close REAL,
                adjusted_close REAL,
                volume INTEGER,
                dividend_amount REAL,
                captured_at TEXT NOT NULL,
                UNIQUE(symbol, week_date)
            );
            """;

    private static final String INSERT_SQL = """
            INSERT OR IGNORE INTO market_commodity_data
            (symbol, week_date, open, high, low, close, adjusted_close, volume, dividend_amount, captured_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final String dbUrl;

    public SqliteMarketStore() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.dbUrl = dotenv.get(ENV_KEY_DB_URL, DEFAULT_DB_URL);
        loadDriver();
    }

    public void initialize() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            statement.execute(CREATE_TABLE_SQL);
            logger.info("Base de datos de mercado inicializada.");

        } catch (SQLException e) {
            logger.error("Error inicializando la base de datos.", e);
        }
    }

    @Override
    public void store(List<MarketData> marketDataList) {
        if (marketDataList == null || marketDataList.isEmpty()) {
            logger.warn("No hay datos para almacenar.");
            return;
        }
        Connection connection = null;
        try {
            connection = connect();
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(INSERT_SQL)) {
                String capturedAt = Instant.now().toString();

                for (MarketData data : marketDataList) {
                    pstmt.setString(1, data.symbol());
                    pstmt.setString(2, data.weekDate());
                    pstmt.setDouble(3, data.open());
                    pstmt.setDouble(4, data.high());
                    pstmt.setDouble(5, data.low());
                    pstmt.setDouble(6, data.close());
                    pstmt.setDouble(7, data.adjustedClose());
                    pstmt.setLong(8, data.volume());
                    pstmt.setDouble(9, data.dividendAmount());
                    pstmt.setString(10, capturedAt);
                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                connection.commit();
                logger.info("Intentados {} registros para almacenamiento.", marketDataList.size());
            }
        } catch (SQLException e) {
            logger.error("Error almacenando datos en SQLite.", e);

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    logger.error("Error al hacer rollback.", rollbackError);
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error cerrando conexión.", e);
                }
            }
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