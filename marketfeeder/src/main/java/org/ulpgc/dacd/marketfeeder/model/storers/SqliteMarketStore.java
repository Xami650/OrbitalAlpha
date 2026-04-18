package org.ulpgc.dacd.marketfeeder.model.storers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.marketfeeder.model.CommoditiesInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

public class SqliteMarketStore implements MarketStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteMarketStore.class);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS market_commodity_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                price_timestamp TEXT NOT NULL,
                open REAL,
                high REAL,
                low REAL,
                close REAL,
                adjusted_close REAL,
                volume INTEGER,
                dividend_amount REAL,
                captured_at TEXT NOT NULL,
                UNIQUE(symbol, price_timestamp)
            );
            """;

    private static final String INSERT_SQL = """
            INSERT OR IGNORE INTO market_commodity_data
            (symbol, price_timestamp, open, high, low, close, adjusted_close, volume, dividend_amount, captured_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final String dbUrl;

    public SqliteMarketStore(String dbUrl) {this.dbUrl = dbUrl;}

    @Override
    public void initialize() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
            logger.info("Market database initialized at {}.", dbUrl);
        } catch (SQLException e) {
            logger.error("Error initializing the database.", e);
        }
    }

    @Override
    public void store(List<CommoditiesInfo> commoditiesInfoList) {
        if (commoditiesInfoList.isEmpty()) return;

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(INSERT_SQL)) {
                String capturedAt = Instant.now().toString();
                for (CommoditiesInfo data : commoditiesInfoList) {
                    populateStatement(pstmt, data, capturedAt);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                connection.commit();
                logger.info("Stored {} records for {}.", commoditiesInfoList.size(), commoditiesInfoList.getFirst().symbol());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Error storing data in SQLite.", e);
        }
    }

    private void populateStatement(PreparedStatement pstmt, CommoditiesInfo data, String capturedAt) throws SQLException {
        pstmt.setString(1, data.symbol());
        pstmt.setString(2, data.priceTimestamp().toString());
        pstmt.setDouble(3, data.open());
        pstmt.setDouble(4, data.high());
        pstmt.setDouble(5, data.low());
        pstmt.setDouble(6, data.close());
        pstmt.setDouble(7, data.adjustedClose());
        pstmt.setLong(8, data.volume());
        pstmt.setDouble(9, data.dividendAmount());
        pstmt.setString(10, capturedAt);
    }

    private Connection connect() throws SQLException { return DriverManager.getConnection(this.dbUrl);}
}
