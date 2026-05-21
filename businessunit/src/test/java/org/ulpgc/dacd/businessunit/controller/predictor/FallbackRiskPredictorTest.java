package org.ulpgc.dacd.businessunit.controller.predictor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.RiskLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FallbackRiskPredictorTest {

    private RiskPredictor primary;
    private RiskPredictor fallback;
    private FallbackRiskPredictor predictor;

    private static final CommodityMetrics METRICS = new CommodityMetrics(
            "WEAT", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    );

    @BeforeEach
    void setUp() {
        primary = mock(RiskPredictor.class);
        fallback = mock(RiskPredictor.class);
        predictor = new FallbackRiskPredictor(primary, fallback);
    }

    @Test
    void predict_whenPrimarySucceeds_returnsPrimaryResultAndFlagIsFalse() {
        CommodityRiskSnapshot expected = new CommodityRiskSnapshot("WEAT", RiskLevel.LOW, 10.0, "Reason");
        when(primary.predict(METRICS)).thenReturn(expected);

        CommodityRiskSnapshot result = predictor.predict(METRICS);

        assertThat(result).isEqualTo(expected);
        assertThat(predictor.isUsingFallback()).isFalse();
        verify(fallback, never()).predict(any());
    }

    @Test
    void predict_whenPrimaryFails_returnsFallbackResultAndFlagIsTrue() {
        CommodityRiskSnapshot fallbackResult = new CommodityRiskSnapshot("WEAT", RiskLevel.MEDIUM, 50.0, "Fallback Reason");
        when(primary.predict(METRICS)).thenThrow(new RuntimeException("API Down"));
        when(fallback.predict(METRICS)).thenReturn(fallbackResult);

        CommodityRiskSnapshot result = predictor.predict(METRICS);

        assertThat(result).isEqualTo(fallbackResult);
        assertThat(predictor.isUsingFallback()).isTrue();
        verify(primary).predict(METRICS);
        verify(fallback).predict(METRICS);
    }
}