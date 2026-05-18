package org.ulpgc.dacd.businessunit.view;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.businessunit.controller.serving.CommodityRiskService;

import java.util.Map;
import java.util.function.BooleanSupplier;

public class BusinessUnitWebServer {

    private static final Logger logger = LoggerFactory.getLogger(BusinessUnitWebServer.class);

    private final int port;
    private final CommodityRiskService commodityRiskService;
    private final BooleanSupplier mlAvailable;
    private Javalin app;

    public BusinessUnitWebServer(int port, CommodityRiskService commodityRiskService, BooleanSupplier mlAvailable) {
        this.port = port;
        this.commodityRiskService = commodityRiskService;
        this.mlAvailable = mlAvailable;
    }

    public void start() {
        app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);

            config.routes.get("/api/health", context -> context.json(Map.of("status", "OK")));

            config.routes.get("/api/status", context ->
                    context.json(Map.of("mlAvailable", mlAvailable.getAsBoolean()))
            );

            config.routes.get("/api/risks", context ->
                    context.json(commodityRiskService.getAllRisks())
            );

            config.routes.get("/api/risks/{commodity}", context -> {
                String commodity = context.pathParam("commodity").toUpperCase();

                context.json(
                        commodityRiskService
                                .getRiskByCommodity(commodity)
                                .orElseThrow(() -> new IllegalArgumentException("Commodity not found: " + commodity))
                );
            });
        });

        app.start(port);
        logger.info("Web server started on port {}", port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            logger.info("Web server stopped");
        }
    }
}
