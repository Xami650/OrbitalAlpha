package org.ulpgc.dacd.weatherfeeder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.config.ConfigLoader;
import org.ulpgc.dacd.weatherfeeder.config.ProducersCsvLoader;
import org.ulpgc.dacd.weatherfeeder.controller.ClimateController;
import org.ulpgc.dacd.weatherfeeder.controller.aggregator.WeatherDataAggregator;
import org.ulpgc.dacd.weatherfeeder.controller.chunker.WeeklyDateChunker;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.NasaPowerClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.ActiveMqEventPublisher;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.EventPublisher;
import org.ulpgc.dacd.weatherfeeder.controller.serializer.EventSerializer;
import org.ulpgc.dacd.weatherfeeder.controller.serializer.GsonEventSerializer;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;
import okhttp3.OkHttpClient;

import java.time.Clock;
import java.time.ZoneOffset;

public class ClimateMain {

    private static final Logger logger = LoggerFactory.getLogger(ClimateMain.class);

    public static void main(String[] args) {
        WeatherConfig config = new ConfigLoader().load();

        ProducersInfo producersInfo = new ProducersInfo(
                new ProducersCsvLoader().load(config.producersFilePath())
        );
        NasaPowerClimateParser parser = new NasaPowerClimateParser(config.sourceSystem());
        OkHttpClient httpClient = new OkHttpClient();
        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder(httpClient, config.api(), parser);
        EventPublisher publisher = new ActiveMqEventPublisher(config.broker().url());
        EventSerializer serializer = new GsonEventSerializer();
        WeatherDataAggregator aggregator = new WeatherDataAggregator(
                config.sourceSystem(),
                config.schedule().expectedDays()
        );
        WeeklyDateChunker chunker = new WeeklyDateChunker(config.schedule().windowDays());
        Clock clock = Clock.system(ZoneOffset.UTC);

        ClimateController controller = new ClimateController(
                feeder, producersInfo, publisher, aggregator, chunker, serializer, config, clock
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing ClimateController...");
            controller.close();
        }));

        controller.start();
    }
}