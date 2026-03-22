package org.ulpgc.dacd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

public class NasaDatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:oraculo_cultivos.db";

    public void initializeDatabase() {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS nasa_weather_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crop_name TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    date TEXT NOT NULL,
                    precipitation REAL,
                    soil_moisture REAL,
                    max_temp REAL,
                    min_temp REAL,
                    captured_at TEXT NOT NULL
                );
                """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println("Base de datos lista con variables de estrés hídrico y térmico.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    public Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("¡ERROR CRÍTICO! Maven no ha cargado la librería SQLite JDBC.");
        }
        return DriverManager.getConnection(DB_URL);
    }

    // NUEVO MÉTODO: Guarda los datos en SQLite
    public void insertWeatherData(List<WeatherData> weatherDataList) {
        String insertSQL = """
                INSERT INTO nasa_weather_data 
                (crop_name, latitude, longitude, date, precipitation, soil_moisture, max_temp, min_temp, captured_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connect();
             PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {

            // Marca de tiempo exacta en la que hacemos la consulta (Requisito del profesor)
            String capturedAt = Instant.now().toString();

            for (WeatherData data : weatherDataList) {
                pstmt.setString(1, data.cropName());
                pstmt.setDouble(2, data.latitude());
                pstmt.setDouble(3, data.longitude());
                pstmt.setString(4, data.date());
                pstmt.setDouble(5, data.precipitation());
                pstmt.setDouble(6, data.soilMoisture());
                pstmt.setDouble(7, data.maxTemp());
                pstmt.setDouble(8, data.minTemp());
                pstmt.setString(9, capturedAt);
                pstmt.addBatch(); // Batch processing: lo mete todo de golpe para que sea ultra rápido
            }

            pstmt.executeBatch();
            System.out.println("Se han insertado " + weatherDataList.size() + " registros en la base de datos.");

        } catch (SQLException e) {
            System.err.println("Error al insertar los datos: " + e.getMessage());
        }
    }
}