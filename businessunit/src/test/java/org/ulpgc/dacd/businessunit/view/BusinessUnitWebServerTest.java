package org.ulpgc.dacd.businessunit.view;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;
import org.ulpgc.dacd.businessunit.controller.serving.ServingDatamart;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessUnitWebServerTest {

    private static BusinessUnitWebServer server;
    private static int port;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startServer() throws Exception {
        port = findAvailablePort();

        InMemoryServingDatamart datamart = new InMemoryServingDatamart();
        datamart.initialize();
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "WEAT", RiskLevel.HIGH, 85.0,
                "HIGH risk due to strong price increase and low precipitation."
        ));
        datamart.saveRiskSnapshot(new CommodityRiskSnapshot(
                "CORN", RiskLevel.LOW, 20.0,
                "LOW risk due to stable prices and adequate rainfall."
        ));

        CommodityRiskService riskService = new CommodityRiskService(datamart);

        server = new BusinessUnitWebServer(port, riskService, () -> true);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthEndpoint_returnsOk() throws Exception {
        HttpResponse<String> response = get("/api/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("OK");
    }

    @Test
    void risksEndpoint_returnsJsonArray() throws Exception {
        HttpResponse<String> response = get("/api/risks");

        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> risks = objectMapper.readValue(
                response.body(), new TypeReference<>() {}
        );

        assertThat(risks).hasSize(2);
        assertThat(risks).extracting(m -> m.get("commodity"))
                .containsExactlyInAnyOrder("WEAT", "CORN");
    }

    @Test
    void riskByCommodity_returnsSnapshotWhenExists() throws Exception {
        HttpResponse<String> response = get("/api/risks/WEAT");

        assertThat(response.statusCode()).isEqualTo(200);

        Map<String, Object> snapshot = objectMapper.readValue(
                response.body(), new TypeReference<>() {}
        );

        assertThat(snapshot.get("commodity")).isEqualTo("WEAT");
        assertThat(snapshot.get("riskLevel")).isEqualTo("HIGH");
        assertThat(((Number) snapshot.get("riskScore")).doubleValue()).isEqualTo(85.0);
        assertThat((String) snapshot.get("reason")).contains("price increase");
    }

    @Test
    void riskByCommodity_isCaseInsensitive() throws Exception {
        HttpResponse<String> response = get("/api/risks/weat");

        assertThat(response.statusCode()).isEqualTo(200);

        Map<String, Object> snapshot = objectMapper.readValue(
                response.body(), new TypeReference<>() {}
        );

        assertThat(snapshot.get("commodity")).isEqualTo("WEAT");
    }

    @Test
    void riskByCommodity_returns400WhenNotFound() throws Exception {
        HttpResponse<String> response = get("/api/risks/UNKNOWN");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Commodity not found");
    }

    @Test
    void statusEndpoint_returnsMlAvailableField() throws Exception {
        HttpResponse<String> response = get("/api/status");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("mlAvailable");

        Map<String, Object> body = objectMapper.readValue(
                response.body(), new TypeReference<>() {}
        );

        assertThat(body.get("mlAvailable")).isEqualTo(true);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class InMemoryServingDatamart implements ServingDatamart {

        private final Map<String, CommodityRiskSnapshot> store = new LinkedHashMap<>();

        @Override
        public void initialize() {
            store.clear();
        }

        @Override
        public void saveRiskSnapshot(CommodityRiskSnapshot snapshot) {
            store.put(snapshot.commodity(), snapshot);
        }

        @Override
        public List<CommodityRiskSnapshot> findAllRiskSnapshots() {
            return List.copyOf(store.values());
        }

        @Override
        public Optional<CommodityRiskSnapshot> findRiskSnapshotByCommodity(String commodity) {
            return Optional.ofNullable(store.get(commodity));
        }
    }
}
