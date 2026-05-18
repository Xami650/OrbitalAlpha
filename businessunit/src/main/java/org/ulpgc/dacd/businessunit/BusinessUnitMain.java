package org.ulpgc.dacd.businessunit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.batch.BatchLayer;
import org.ulpgc.dacd.businessunit.controller.batch.EventStoreReader;
import org.ulpgc.dacd.businessunit.controller.batch.FileEventStoreReader;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfig;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfigLoader;
import org.ulpgc.dacd.businessunit.controller.datamart.BatchDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.SqliteBatchDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.SqliteServingDatamart;
import org.ulpgc.dacd.businessunit.controller.predictor.FallbackRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.predictor.HeuristicRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.predictor.MlApiRiskPredictor;
import org.ulpgc.dacd.businessunit.controller.processor.CommodityPriceEventProcessor;
import org.ulpgc.dacd.businessunit.controller.processor.WeatherEventProcessor;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;
import org.ulpgc.dacd.businessunit.controller.serving.ServingLayer;
import org.ulpgc.dacd.businessunit.controller.speed.ActiveMqEventSubscriber;
import org.ulpgc.dacd.businessunit.controller.speed.EventSubscriber;
import org.ulpgc.dacd.businessunit.controller.speed.SpeedLayer;
import org.ulpgc.dacd.businessunit.view.BusinessUnitWebServer;

public class BusinessUnitMain {

    private static final Logger logger = LoggerFactory.getLogger(BusinessUnitMain.class);
    private static final String CONFIG_FILE = "businessunit.properties";

    public static void main(String[] args) {
        logger.info("Starting BusinessUnit...");
        try {
            BusinessUnitConfig config = loadConfig();
            logger.info("Configuration loaded. API port: {}, ML API: {}", config.apiPort(), config.mlApiUrl());

            BatchDatamart batchDatamart = createBatchDatamart(config);
            BatchLayer batchLayer = createBatchLayer(config, batchDatamart);
            batchLayer.rebuild();

            ServingDatamart servingDatamart = createServingDatamart(config);
            FallbackRiskPredictor predictor = createPredictor(config);
            ServingLayer servingLayer = createServingLayer(batchDatamart, servingDatamart, predictor);
            servingLayer.rebuild();

            SpeedLayer speedLayer = createSpeedLayer(config, batchLayer, servingLayer);
            CommodityRiskService commodityRiskService = new CommodityRiskService(servingDatamart);
            BusinessUnitWebServer webServer = new BusinessUnitWebServer(
                    config.apiPort(),
                    commodityRiskService,
                    () -> !predictor.isUsingFallback()
            );

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                speedLayer.stop();
                webServer.stop();
            }));

            speedLayer.start();
            webServer.start();
            logger.info("BusinessUnit started successfully");
        } catch (Exception e) {
            logger.error("Failed to start BusinessUnit", e);
        }
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

    private static FallbackRiskPredictor createPredictor(BusinessUnitConfig config) {
        return new FallbackRiskPredictor(
                new MlApiRiskPredictor(config.mlApiUrl()),
                new HeuristicRiskPredictor()
        );
    }

    private static ServingLayer createServingLayer(
            BatchDatamart batchDatamart,
            ServingDatamart servingDatamart,
            FallbackRiskPredictor predictor
    ) {
        return new ServingLayer(
                batchDatamart,
                servingDatamart,
                predictor,
                new CommodityPriceEventProcessor(),
                new WeatherEventProcessor()
        );
    }

    private static SpeedLayer createSpeedLayer(
            BusinessUnitConfig config,
            BatchLayer batchLayer,
            ServingLayer servingLayer
    ) {
        EventSubscriber eventSubscriber = new ActiveMqEventSubscriber(
                config.brokerUrl(),
                config.topics()
        );

        return new SpeedLayer(
                eventSubscriber,
                batchLayer,
                servingLayer
        );
    }
}
