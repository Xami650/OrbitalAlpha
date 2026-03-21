package org.ulpgc.dacd;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NasaApiClient {

    private final OkHttpClient client;

    public NasaApiClient() {
        this.client = new OkHttpClient();
    }

    public List<WeatherData> getWeatherData(String cropName, double lat, double lon, String startDate, String endDate) {

        // Añadimos T2M_MAX y T2M_MIN a la petición
        String url = String.format(
                "https://power.larc.nasa.gov/api/temporal/daily/point?parameters=PRECTOTCORR,GWETROOT,T2M_MAX,T2M_MIN&community=AG&longitude=%s&latitude=%s&start=%s&end=%s&format=JSON",
                lon, lat, startDate, endDate
        );

        Request request = new Request.Builder().url(url).build();
        List<WeatherData> weatherDataList = new ArrayList<>();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.err.println("Error en la API de la NASA: Código " + response.code());
                return weatherDataList;
            }

            String jsonResponse = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject parameters = jsonObject.getAsJsonObject("properties").getAsJsonObject("parameter");

            JsonObject precipitationData = parameters.getAsJsonObject("PRECTOTCORR");
            JsonObject soilMoistureData = parameters.getAsJsonObject("GWETROOT");
            JsonObject maxTempData = parameters.getAsJsonObject("T2M_MAX");
            JsonObject minTempData = parameters.getAsJsonObject("T2M_MIN");

            for (String date : precipitationData.keySet()) {
                double precip = precipitationData.get(date).getAsDouble();
                double soilMoist = soilMoistureData.get(date).getAsDouble();
                double maxTemp = maxTempData.get(date).getAsDouble();
                double minTemp = minTempData.get(date).getAsDouble();

                weatherDataList.add(new WeatherData(cropName, lat, lon, date, precip, soilMoist, maxTemp, minTemp));
            }

        } catch (IOException e) {
            System.err.println("Fallo de conexión con la API: " + e.getMessage());
        }

        return weatherDataList;
    }
}