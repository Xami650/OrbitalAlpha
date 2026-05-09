package org.ulpgc.dacd.businessunit;

import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfig;
import org.ulpgc.dacd.businessunit.controller.config.BusinessUnitConfigLoader;
import org.ulpgc.dacd.businessunit.controller.datamart.ServingDatamart;
import org.ulpgc.dacd.businessunit.controller.datamart.SqliteServingDatamart;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;
import org.ulpgc.dacd.businessunit.view.BusinessUnitWebServer;

public class BusinessUnitMain {

    private static final String CONFIG_FILE = "businessunit.properties";

    public static void main(String[] args) {
        BusinessUnitConfig config = new BusinessUnitConfigLoader(CONFIG_FILE).load();

        ServingDatamart servingDatamart = new SqliteServingDatamart(config.servingDatamartUrl());
        servingDatamart.initialize();

        CommodityRiskService commodityRiskService = new CommodityRiskService(servingDatamart);
        commodityRiskService.initializeDemoData();

        BusinessUnitWebServer webServer = new BusinessUnitWebServer(
                config.apiPort(),
                commodityRiskService
        );

        webServer.start();
    }
}