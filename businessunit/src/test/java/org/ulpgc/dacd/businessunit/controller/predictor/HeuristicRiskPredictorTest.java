package org.ulpgc.dacd.businessunit.controller.predictor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicRiskPredictorTest {

    private HeuristicRiskPredictor predictor;

    @BeforeEach
    void setUp() {
        predictor = new HeuristicRiskPredictor();
    }

    private CommodityMetrics neutral() {
        return new CommodityMetrics(
                "WEAT", 100.0, 100.0,
                0.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );
    }

    private CommodityMetrics withOverrides(
            double priceChangePercent,
            double precipitation,
            double rootZoneSoilWetness,
            double temperatureMax,
            double temperatureMin,
            double priceVolatility,
            double priceTrend,
            double precipitationDelta,
            double soilWetnessDelta,
            double temperatureMaxDelta
    ) {
        return new CommodityMetrics(
                "WEAT", 100.0, 100.0,
                priceChangePercent,
                precipitation,
                rootZoneSoilWetness,
                temperatureMax,
                temperatureMin,
                priceVolatility,
                priceTrend,
                precipitationDelta,
                soilWetnessDelta,
                temperatureMaxDelta
        );
    }

    @Test
    void allNeutral_producesLowRiskWithZeroScore() {
        CommodityRiskSnapshot result = predictor.predict(neutral());

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.riskScore()).isEqualTo(0.0);
        assertThat(result.commodity()).isEqualTo("WEAT");
        assertThat(result.reason()).contains("LOW");
        assertThat(result.reason()).contains("stable");
    }

    @Test
    void highPriceChangePercent_contributes30Points() {
        CommodityMetrics metrics = withOverrides(
                6.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(30.0);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW_MEDIUM);
    }

    @Test
    void moderatePriceChangePercent_contributes15Points() {
        CommodityMetrics metrics = withOverrides(
                3.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(15.0);
    }

    @Test
    void lowPrecipitation_contributes15Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 0.5, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(15.0);
    }

    @Test
    void lowSoilWetness_contributes20Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.2, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(20.0);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW_MEDIUM);
    }

    @Test
    void highTemperatureMax_contributes15Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 35.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(15.0);
    }

    @Test
    void lowTemperatureMin_contributes15Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 1.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(15.0);
    }

    @Test
    void highPriceVolatility_contributes10Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 10.0,
                5.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(10.0);
    }

    @Test
    void highPriceTrend_contributes10Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 4.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(10.0);
    }

    @Test
    void precipitationDeltaBelowThreshold_contributes5Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, -3.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(5.0);
    }

    @Test
    void soilWetnessDeltaBelowThreshold_contributes5Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, -0.2, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(5.0);
    }

    @Test
    void temperatureMaxDeltaAboveThreshold_contributes5Points() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 6.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(5.0);
    }

    @Test
    void multipleTriggersProduceCorrectCumulativeScore() {
        CommodityMetrics metrics = withOverrides(
                6.0, 0.5, 0.2, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(65.0);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM_HIGH);
    }

    @Test
    void scoreCappedAt100WhenAllTriggersActive() {
        CommodityMetrics metrics = withOverrides(
                6.0, 0.5, 0.2, 35.0, 1.0,
                5.0, 4.0, -3.0, -0.2, 6.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(100.0);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void lowRiskLevel_scoreBelow20() {
        CommodityRiskSnapshot result = predictor.predict(neutral());

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.riskScore()).isLessThan(20.0);
    }

    @Test
    void lowMediumRiskLevel_scoreBetween20And39() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.2, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW_MEDIUM);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(20.0);
        assertThat(result.riskScore()).isLessThan(40.0);
    }

    @Test
    void mediumRiskLevel_scoreBetween40And59() {
        CommodityMetrics metrics = withOverrides(
                0.0, 0.5, 0.2, 25.0, 10.0,
                1.0, 1.0, -3.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(40.0);
        assertThat(result.riskScore()).isLessThan(60.0);
    }

    @Test
    void mediumHighRiskLevel_scoreBetween60And79() {
        CommodityMetrics metrics = withOverrides(
                6.0, 0.5, 0.2, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM_HIGH);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(60.0);
        assertThat(result.riskScore()).isLessThan(80.0);
    }

    @Test
    void highRiskLevel_scoreAtLeast80() {
        CommodityMetrics metrics = withOverrides(
                6.0, 0.5, 0.2, 35.0, 1.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    void reasonContainsStrongPriceIncrease_whenPriceChangeAbove5() {
        CommodityMetrics metrics = withOverrides(
                6.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("strong price increase");
    }

    @Test
    void reasonContainsModeratePriceIncrease_whenPriceChangeBetween2And5() {
        CommodityMetrics metrics = withOverrides(
                3.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("moderate price increase");
    }

    @Test
    void reasonContainsLowPrecipitation_whenPrecipitationBelow1() {
        CommodityMetrics metrics = withOverrides(
                0.0, 0.5, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("low precipitation");
    }

    @Test
    void reasonContainsLowSoilWetness_whenSoilWetnessInRange() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.2, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("low root-zone soil wetness");
    }

    @Test
    void reasonContainsHighMaxTemperature_whenTempMaxAbove32() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 35.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("high maximum temperature");
    }

    @Test
    void reasonContainsLowMinTemperature_whenTempMinBelow3() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.5, 25.0, 1.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("low minimum temperature");
    }

    @Test
    void reasonContainsMultipleTriggers_whenMultipleConditionsMet() {
        CommodityMetrics metrics = withOverrides(
                6.0, 0.5, 0.2, 35.0, 1.0,
                5.0, 4.0, -3.0, -0.2, 6.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.reason()).contains("strong price increase");
        assertThat(result.reason()).contains("high price volatility");
        assertThat(result.reason()).contains("sustained upward price trend");
        assertThat(result.reason()).contains("low precipitation");
        assertThat(result.reason()).contains("precipitation below recent average");
        assertThat(result.reason()).contains("low root-zone soil wetness");
        assertThat(result.reason()).contains("soil wetness below recent average");
        assertThat(result.reason()).contains("high maximum temperature");
        assertThat(result.reason()).contains("temperature above recent average");
        assertThat(result.reason()).contains("low minimum temperature");
    }

    @Test
    void soilWetnessExactlyZero_doesNotContribute() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.0, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(0.0);
    }

    @Test
    void soilWetnessExactlyAtThreshold_doesNotContribute() {
        CommodityMetrics metrics = withOverrides(
                0.0, 5.0, 0.35, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(0.0);
    }

    @Test
    void priceChangeExactly2_doesNotContribute() {
        CommodityMetrics metrics = withOverrides(
                2.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(0.0);
    }

    @Test
    void priceChangeExactly5_contributes15NotThirty() {
        CommodityMetrics metrics = withOverrides(
                5.0, 5.0, 0.5, 25.0, 10.0,
                1.0, 1.0, 0.0, 0.0, 0.0
        );

        CommodityRiskSnapshot result = predictor.predict(metrics);

        assertThat(result.riskScore()).isEqualTo(15.0);
    }
}
