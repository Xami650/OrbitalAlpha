package org.ulpgc.dacd;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando OrbitalAlpha... Motor de automatización multizona activado.");

        NasaDatabaseManager dbManager = new NasaDatabaseManager();
        dbManager.initializeDatabase();
        NasaApiClient apiClient = new NasaApiClient();

        // 1. Molde para nuestras ubicaciones
        record CropLocation(String name, double lat, double lon) {}

        // 2. Nuestra cartera de fincas premium (Granularidad máxima)
        List<CropLocation> portfolio = List.of(
                // --- CAFÉ ARÁBICA (Minas Gerais, Brasil) ---
                new CropLocation("Café - Faz. Sertãozinho (Botelhos)", -21.63, -46.39),
                new CropLocation("Café - Monte Alegre (Conc. dos Ouros)", -22.43, -45.79),
                new CropLocation("Café - Faz. Monte Verde (Ouro Fino)", -22.28, -46.36),
                new CropLocation("Café - Faz. Barinas (Araxá)", -19.59, -46.94),
                new CropLocation("Café - Faz. Campo Verde (Campo do Meio)", -21.11, -45.82),
                new CropLocation("Café - Faz. Sertão (Carmo de Minas)", -22.12, -45.12),

                // --- CACAO (Región de Lacs, Costa de Marfil) ---
                new CropLocation("Cacao - Cayatt (Yamoussoukro)", 6.82, -5.27),
                new CropLocation("Cacao - Zona de Djekanou", 6.40, -5.13),
                new CropLocation("Cacao - Cooperativas (Toumodi/Lacs)", 6.55, -5.01),

                // --- AOVE PREMIUM (Jaén, España) ---
                new CropLocation("AOVE - Campos de Biatia (Baeza)", 37.99, -3.46),
                new CropLocation("AOVE - Oro de Cánava (Jimena)", 37.84, -3.47),
                new CropLocation("AOVE - Puerta de las Villas (Mogón)", 38.06, -3.02),
                new CropLocation("AOVE - Melgarejo (Pegalajar)", 37.74, -3.65),
                new CropLocation("AOVE - Finca Badenes (Lopera)", 37.94, -4.11),
                new CropLocation("AOVE - Verde Salud (Sierra Mágina)", 37.83, -3.43),
                new CropLocation("AOVE - Jabalcuz (Sierra Pandera)", 37.62, -3.78)
        );

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable fetchAndStoreTask = () -> {
            System.out.println("\n--- Iniciando escaneo de microclimas (" + java.time.LocalDateTime.now() + ") ---");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String startDate = LocalDate.now().minusDays(7).format(formatter);
            String endDate = LocalDate.now().minusDays(3).format(formatter);

            for (CropLocation crop : portfolio) {
                System.out.println("-> Extrayendo datos: " + crop.name() + "...");
                List<WeatherData> data = apiClient.getWeatherData(crop.name(), crop.lat(), crop.lon(), startDate, endDate);

                if (!data.isEmpty()) {
                    dbManager.insertWeatherData(data);
                } else {
                    System.out.println("⚠️ Sin datos para " + crop.name());
                }
            }
            System.out.println("--- Ciclo finalizado. Modo reposo durante 6 horas ---");
        };

        // Ejecutar ahora y repetir cada 6 horas
        scheduler.scheduleAtFixedRate(fetchAndStoreTask, 0, 6, TimeUnit.HOURS);
    }
}