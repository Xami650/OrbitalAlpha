package org.ulpgc.dacd.businessunit.controller.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class MlApiContractTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonNode contract;

    @BeforeAll
    static void loadContract() throws Exception {
        try (InputStream is = MlApiContractTest.class.getResourceAsStream("/predict-contract.json")) {
            contract = mapper.readTree(is);
        }
    }

    @Test
    void requestBodyContainsAllContractFields() throws Exception {
        CommodityMetrics metrics = new CommodityMetrics(
                "WEAT", 5.80, 5.43, 6.8, 0.2, 0.28, 34.0, 18.0,
                3.5, 1.2, -1.5, -0.1, 4.0
        );

        String body = mapper.writeValueAsString(Map.ofEntries(
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

        JsonNode requestNode = mapper.readTree(body);
        Set<String> contractFields = fieldNames(contract.get("request").get("requiredFields"));

        Set<String> actualFields = fieldNames(requestNode);

        assertThat(actualFields).containsAll(contractFields);
    }

    @Test
    void contractResponseFieldsMatchJavaExpectations() {
        Set<String> responseFields = fieldNames(contract.get("response").get("requiredFields"));

        assertThat(responseFields).containsExactlyInAnyOrder(
                "commodity", "riskLevel", "riskScore", "reason"
        );
    }

    @Test
    void contractRiskLevelsMatchJavaEnum() {
        Set<String> contractLevels = StreamSupport.stream(
                        contract.get("response").get("validRiskLevels").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toSet());

        Set<String> javaLevels = Set.of("LOW", "LOW_MEDIUM", "MEDIUM", "MEDIUM_HIGH", "HIGH");

        assertThat(contractLevels).isEqualTo(javaLevels);
    }

    private static Set<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(
                ((Iterable<String>) node::fieldNames).spliterator(), false
        ).collect(Collectors.toSet());
    }
}
