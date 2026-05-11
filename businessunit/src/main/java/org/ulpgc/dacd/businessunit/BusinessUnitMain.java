package org.ulpgc.dacd.businessunit;

import org.ulpgc.dacd.businessunit.controller.batch.BatchLayer;
import org.ulpgc.dacd.businessunit.controller.batch.EventStoreReader;
import org.ulpgc.dacd.businessunit.controller.batch.FileEventStoreReader;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfig;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfigLoader;
import org.ulpgc.dacd.businessunit.controller.datamart.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.SqliteBatchDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.SqliteServingDatamart;
import org.ulpgc.dacd.businessunit.controller.predictor.HeuristicRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.predictor.RiskPredictor;
import org.ulpgc.dacd.businessunit.controller.processor.CommodityPriceEventProcessor;
import org.ulpgc.dacd.businessunit.controller.processor.WeatherEventProcessor;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;
import org.ulpgc.dacd.businessunit.controller.speed.ActiveMqEventSubscriber;
import org.ulpgc.dacd.businessunit.controller.speed.EventSubscriber;
import org.ulpgc.dacd.businessunit.controller.speed.SpeedLayer;
import org.ulpgc.dacd.businessunit.view.BusinessUnitWebServer;

public class BusinessUnitMain {

    private static final String CONFIG_FILE = "businessunit.properties";

    public static void main(String[] args) {
        BusinessUnitConfig config = loadConfig();

        BatchDatamart batchDatamart = createBatchDatamart(config);
        BatchLayer batchLayer = createBatchLayer(config, batchDatamart);
        batchLayer.rebuild();

        ServingDatamart servingDatamart = createServingDatamart(config);
        ServingLayer servingLayer = createServingLayer(batchDatamart, servingDatamart);
        servingLayer.rebuild();

        SpeedLayer speedLayer = createSpeedLayer(config, batchLayer, servingLayer);
        speedLayer.start();

        CommodityRiskService commodityRiskService = new CommodityRiskService(servingDatamart);
        BusinessUnitWebServer webServer = new BusinessUnitWebServer(
                config.apiPort(),
                commodityRiskService
        );

        webServer.start();
    }

    private static BusinessUnitConfig loadConfig() {
        return new BusinessUnitConfigLoader(CONFIG_FILE).load();
    }

    private static BatchDatamart createBatchDatamart(BusinessUnitConfig config) {
        BatchDatamart batchDatamart = new SqliteBatchDatamart(config.batchDatamartUrl());
        batchDatamart.initialize();
        return batchDatamart;
    }

    private static BatchLayer createBatchLayer(BusinessUnitConfig config, BatchDatamart batchDatamart) {
        EventStoreReader eventStoreReader = new FileEventStoreReader(config.eventStorePath());
        return new BatchLayer(eventStoreReader, batchDatamart);
    }

    private static ServingDatamart createServingDatamart(BusinessUnitConfig config) {
        ServingDatamart servingDatamart = new SqliteServingDatamart(config.servingDatamartUrl());
        servingDatamart.initialize();
        return servingDatamart;
    }

    private static ServingLayer createServingLayer(
            BatchDatamart batchDatamart,
            ServingDatamart servingDatamart
    ) {
        RiskPredictor riskPredictor = new HeuristicRiskPredictor();

        CommodityPriceEventProcessor commodityPriceEventProcessor =
                new CommodityPriceEventProcessor();

        WeatherEventProcessor weatherEventProcessor =
                new WeatherEventProcessor();

        return new ServingLayer(
                batchDatamart,
                servingDatamart,
                riskPredictor,
                commodityPriceEventProcessor,
                weatherEventProcessor
        );
    }

    private static SpeedLayer createSpeedLayer(
            BusinessUnitConfig config,
            BatchLayer batchLayer,
            ServingLayer servingLayer
    ) {
        EventSubscriber eventSubscriber = new ActiveMqEventSubscriber(
                config.brokerUrl(),
                config.clientId(),
                config.topics()
        );

        return new SpeedLayer(
                eventSubscriber,
                batchLayer,
                servingLayer
        );
    }
}