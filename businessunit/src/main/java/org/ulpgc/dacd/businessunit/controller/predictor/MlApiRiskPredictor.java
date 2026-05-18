package org.ulpgc.dacd.businessunit.controller.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(MlApiRiskPredictor.class);

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
            logger.debug("Calling ML API for commodity {}", metrics.commodity());
            String body = objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("commodity", metrics.commodity()),
                    Map.entry("priceChangePercent", metrics.priceChangePercent()),
                    Map.entry("precipitation", metrics.precipitation()),
                    Map.entry("rootZoneSoilWetness", metrics.rootZoneSoilWetness()),
                    Map.entry("temperatureMax", metrics.temperatureMax()),
                    Map.entry("temperatureMin", metrics.temperatureMin()),
                    Map.entry("priceVolatility", metrics.priceVolatility()),
                    Map.entry("priceTrend", metrics.priceTrend()),
                    Map.entry("precipitationDelta", metrics.precipitationDelta()),
                    Map.entry("soilWetnessDelta", metrics.soilWetnessDelta()),
                    Map.entry("temperatureMaxDelta", metrics.temperatureMaxDelta())
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlApiUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("ML API returned HTTP {} for {}", response.statusCode(), metrics.commodity());
                throw new RuntimeException("ML API returned HTTP " + response.statusCode());
            }

            JsonNode node = objectMapper.readTree(response.body());
            CommodityRiskSnapshot snapshot = new CommodityRiskSnapshot(
                    metrics.commodity(),
                    RiskLevel.valueOf(node.get("riskLevel").asText()),
                    node.get("riskScore").asDouble(),
                    node.get("reason").asText()
            );
            logger.info("ML prediction for {}: {} (score={})", metrics.commodity(), snapshot.riskLevel(), snapshot.riskScore());
            return snapshot;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("ML API call failed for {}", metrics.commodity(), e);
            throw new RuntimeException("ML API call failed for " + metrics.commodity(), e);
        }
    }
}
