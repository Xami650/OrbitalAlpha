package org.ulpgc.dacd.businessunit.controller.serving;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.datamart.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.controller.predictor.RiskPredictor;
import org.ulpgc.dacd.businessunit.controller.processor.CommodityPriceEventProcessor;
import org.ulpgc.dacd.businessunit.controller.processor.WeatherEventProcessor;
import org.ulpgc.dacd.businessunit.model.CommodityMetrics;
import org.ulpgc.dacd.businessunit.model.CommodityRiskSnapshot;
import org.ulpgc.dacd.businessunit.model.events.HistoricalEvent;

import java.util.List;
import java.util.Map;

public class ServingLayer {

    private static final Logger logger = LoggerFactory.getLogger(ServingLayer.class);

    private final BatchDatamart batchDatamart;
    private final ServingDatamart servingDatamart;
    private final RiskPredictor riskPredictor;
    private final CommodityPriceEventProcessor commodityPriceEventProcessor;
    private final WeatherEventProcessor weatherEventProcessor;

    public ServingLayer(
            BatchDatamart batchDatamart,
            ServingDatamart servingDatamart,
            RiskPredictor riskPredictor,
            CommodityPriceEventProcessor commodityPriceEventProcessor,
            WeatherEventProcessor weatherEventProcessor
    ) {
        this.batchDatamart = batchDatamart;
        this.servingDatamart = servingDatamart;
        this.riskPredictor = riskPredictor;
        this.commodityPriceEventProcessor = commodityPriceEventProcessor;
        this.weatherEventProcessor = weatherEventProcessor;
    }

    public void rebuild() {
        logger.info("Starting serving layer rebuild...");

        List<HistoricalEvent> historicalEvents = batchDatamart.findAllHistoricalEvents();

        Map<String, CommodityPriceEventProcessor.PriceMetrics> priceMetricsByCommodity =
                commodityPriceEventProcessor.process(historicalEvents);

        Map<String, WeatherEventProcessor.WeatherMetrics> weatherMetricsByCommodity =
                weatherEventProcessor.process(historicalEvents);

        priceMetricsByCommodity.forEach((commodity, priceMetrics) -> {
            WeatherEventProcessor.WeatherMetrics weatherMetrics =
                    weatherMetricsByCommodity.getOrDefault(
                            commodity,
                            WeatherEventProcessor.WeatherMetrics.empty()
                    );

            CommodityMetrics metrics = new CommodityMetrics(
                    commodity,
                    priceMetrics.latestClosePrice(),
                    priceMetrics.previousClosePrice(),
                    priceMetrics.priceChangePercent(),
                    weatherMetrics.precipitation(),
                    weatherMetrics.rootZoneSoilWetness(),
                    weatherMetrics.temperatureMax(),
                    weatherMetrics.temperatureMin()
            );

            CommodityRiskSnapshot snapshot = riskPredictor.predict(metrics);
            servingDatamart.saveRiskSnapshot(snapshot);
        });

        logger.info("Serving layer rebuild finished. Updated {} commodities.", priceMetricsByCommodity.size());
    }
}
