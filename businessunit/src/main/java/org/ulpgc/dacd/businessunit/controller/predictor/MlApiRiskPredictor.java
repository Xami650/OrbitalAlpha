package org.ulpgc.dacd.businessunit.controller.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class MlApiRiskPredictor implements RiskPredictor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String mlApiUrl;
    private final HttpClient httpClient;

    public MlApiRiskPredictor(String mlApiUrl) {
        this.mlApiUrl = mlApiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public CommodityRiskSnapshot predict(CommodityMetrics metrics) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "commodity", metrics.commodity(),
                    "priceChangePercent", metrics.priceChangePercent(),
                    "precipitation", metrics.precipitation(),
                    "rootZoneSoilWetness", metrics.rootZoneSoilWetness(),
                    "temperatureMax", metrics.temperatureMax(),
                    "temperatureMin", metrics.temperatureMin()
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlApiUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("ML API returned HTTP " + response.statusCode());
            }

            JsonNode node = objectMapper.readTree(response.body());
            return new CommodityRiskSnapshot(
                    metrics.commodity(),
                    RiskLevel.valueOf(node.get("riskLevel").asText()),
                    node.get("riskScore").asDouble(),
                    node.get("reason").asText()
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("ML API call failed for " + metrics.commodity(), e);
        }
    }
}
