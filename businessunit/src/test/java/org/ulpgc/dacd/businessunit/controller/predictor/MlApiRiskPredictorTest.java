package org.ulpgc.dacd.businessunit.controller.predictor;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MlApiRiskPredictorTest {

    private HttpServer server;
    private String baseUrl;

    private static final CommodityMetrics SAMPLE_METRICS = new CommodityMetrics(
            "WEAT", 5.80, 5.43, 6.8, 0.2, 0.28, 34.0, 18.0,
            3.5, 1.2, -1.5, -0.1, 4.0
    );

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void constructorAcceptsUrlWithoutError() {
        MlApiRiskPredictor predictor = new MlApiRiskPredictor("http://localhost:8000");
        assertThat(predictor).isNotNull();
    }

    @Test
    void parsesHighRiskResponseCorrectly() {
        String responseBody = """
                {
                    "commodity": "WEAT",
                    "riskLevel": "HIGH",
                    "riskScore": 90.0,
                    "reason": "HIGH risk due to strong price increase, low precipitation."
                }
                """;

        server.createContext("/predict", exchange -> {
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);
        CommodityRiskSnapshot result = predictor.predict(SAMPLE_METRICS);

        assertThat(result.commodity()).isEqualTo("WEAT");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.riskScore()).isEqualTo(90.0);
        assertThat(result.reason()).isEqualTo("HIGH risk due to strong price increase, low precipitation.");
    }

    @Test
    void parsesLowRiskResponseCorrectly() {
        String responseBody = """
                {
                    "commodity": "WEAT",
                    "riskLevel": "LOW",
                    "riskScore": 10.0,
                    "reason": "LOW risk: market and weather indicators remain stable."
                }
                """;

        server.createContext("/predict", exchange -> {
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);
        CommodityRiskSnapshot result = predictor.predict(SAMPLE_METRICS);

        assertThat(result.commodity()).isEqualTo("WEAT");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.riskScore()).isEqualTo(10.0);
        assertThat(result.reason()).contains("stable");
    }

    @Test
    void parsesMediumRiskResponseCorrectly() {
        String responseBody = """
                {
                    "commodity": "CORN",
                    "riskLevel": "MEDIUM",
                    "riskScore": 55.0,
                    "reason": "MEDIUM risk due to moderate price increase."
                }
                """;

        server.createContext("/predict", exchange -> {
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        CommodityMetrics cornMetrics = new CommodityMetrics(
                "CORN", 4.50, 4.30, 4.6, 1.5, 0.4, 30.0, 15.0,
                2.0, 1.5, 0.0, 0.0, 0.0
        );

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);
        CommodityRiskSnapshot result = predictor.predict(cornMetrics);

        assertThat(result.commodity()).isEqualTo("CORN");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.riskScore()).isEqualTo(55.0);
    }

    @Test
    void throwsRuntimeExceptionOnNon200Response() {
        server.createContext("/predict", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);

        assertThatThrownBy(() -> predictor.predict(SAMPLE_METRICS))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ML API returned HTTP 500");
    }

    @Test
    void throwsRuntimeExceptionWhenServerUnreachable() {
        server.stop(0);

        MlApiRiskPredictor predictor = new MlApiRiskPredictor("http://localhost:1");

        assertThatThrownBy(() -> predictor.predict(SAMPLE_METRICS))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sendsRequestBodyWithCorrectFields() {
        StringBuilder capturedBody = new StringBuilder();

        server.createContext("/predict", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            capturedBody.append(body);

            String responseBody = """
                    {
                        "commodity": "WEAT",
                        "riskLevel": "HIGH",
                        "riskScore": 85.0,
                        "reason": "HIGH risk."
                    }
                    """;
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);
        predictor.predict(SAMPLE_METRICS);

        String sent = capturedBody.toString();
        assertThat(sent).contains("\"commodity\"");
        assertThat(sent).contains("\"WEAT\"");
        assertThat(sent).contains("\"priceChangePercent\"");
        assertThat(sent).contains("\"precipitation\"");
        assertThat(sent).contains("\"rootZoneSoilWetness\"");
        assertThat(sent).contains("\"temperatureMax\"");
        assertThat(sent).contains("\"temperatureMin\"");
        assertThat(sent).contains("\"priceVolatility\"");
        assertThat(sent).contains("\"priceTrend\"");
        assertThat(sent).contains("\"precipitationDelta\"");
        assertThat(sent).contains("\"soilWetnessDelta\"");
        assertThat(sent).contains("\"temperatureMaxDelta\"");
    }

    @Test
    void sendsPostRequestWithJsonContentType() {
        StringBuilder capturedMethod = new StringBuilder();
        StringBuilder capturedContentType = new StringBuilder();

        server.createContext("/predict", exchange -> {
            capturedMethod.append(exchange.getRequestMethod());
            String ct = exchange.getRequestHeaders().getFirst("Content-Type");
            if (ct != null) capturedContentType.append(ct);

            String responseBody = """
                    {
                        "commodity": "WEAT",
                        "riskLevel": "LOW",
                        "riskScore": 5.0,
                        "reason": "LOW risk."
                    }
                    """;
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        MlApiRiskPredictor predictor = new MlApiRiskPredictor(baseUrl);
        predictor.predict(SAMPLE_METRICS);

        assertThat(capturedMethod.toString()).isEqualTo("POST");
        assertThat(capturedContentType.toString()).isEqualTo("application/json");
    }

    @Test
    void parsesAllRiskLevelsFromResponse() {
        for (RiskLevel level : RiskLevel.values()) {
            HttpServer levelServer;
            try {
                levelServer = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int port = levelServer.getAddress().getPort();

            String responseBody = String.format("""
                    {
                        "commodity": "WEAT",
                        "riskLevel": "%s",
                        "riskScore": 50.0,
                        "reason": "%s risk."
                    }
                    """, level.name(), level.name());

            levelServer.createContext("/predict", exchange -> {
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });
            levelServer.start();

            try {
                MlApiRiskPredictor predictor = new MlApiRiskPredictor("http://localhost:" + port);
                CommodityRiskSnapshot result = predictor.predict(SAMPLE_METRICS);
                assertThat(result.riskLevel()).isEqualTo(level);
            } finally {
                levelServer.stop(0);
            }
        }
    }
}
