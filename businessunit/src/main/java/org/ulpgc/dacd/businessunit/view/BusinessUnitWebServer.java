package org.ulpgc.dacd.businessunit.view;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.bundled.CorsPluginConfig;
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

            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(CorsPluginConfig.CorsRule::anyHost)
            );

            config.routes.exception(IllegalArgumentException.class, (e, context) -> {
                context.status(400);
                context.json(Map.of("error", e.getMessage()));
            });

            config.routes.exception(Exception.class, (e, context) -> {
                logger.error("Unhandled exception on {} {}", context.method(), context.path(), e);
                context.status(500);
                context.json(Map.of("error", "Internal server error"));
            });

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
