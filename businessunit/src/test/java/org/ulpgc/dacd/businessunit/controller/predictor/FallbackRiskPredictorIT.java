package org.ulpgc.dacd.businessunit.controller.predictor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackRiskPredictorIT {

    private static final CommodityMetrics WEAT_METRICS = new CommodityMetrics(
            "WEAT", 5.80, 5.43, 6.8, 0.2, 0.28, 34.0, 18.0
    );

    @Test
    void whenMlApiUnreachable_usesFallbackAndFlagIsTrue() {
        FallbackRiskPredictor predictor = new FallbackRiskPredictor(
                new MlApiRiskPredictor("http://localhost:9999"),
                new HeuristicRiskPredictor()
        );

        CommodityRiskSnapshot result = predictor.predict(WEAT_METRICS);

        assertThat(predictor.isUsingFallback()).isTrue();
        assertThat(result.commodity()).isEqualTo("WEAT");
        assertThat(result.riskLevel()).isIn(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(result.reason()).isNotBlank();
        System.out.println("[FALLBACK] " + result.riskLevel() + " / score=" + result.riskScore() + " / " + result.reason());
    }

    @Test
    @EnabledIf("isMlApiAvailable")
    void whenMlApiAvailable_usesMlAndFlagIsFalse() {
        FallbackRiskPredictor predictor = new FallbackRiskPredictor(
                new MlApiRiskPredictor("http://localhost:8000"),
                new HeuristicRiskPredictor()
        );

        CommodityRiskSnapshot result = predictor.predict(WEAT_METRICS);

        assertThat(predictor.isUsingFallback()).isFalse();
        assertThat(result.commodity()).isEqualTo("WEAT");
        assertThat(result.riskLevel()).isIn(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(result.reason()).isNotBlank();
        System.out.println("[ML]       " + result.riskLevel() + " / score=" + result.riskScore() + " / " + result.reason());
    }

    static boolean isMlApiAvailable() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
