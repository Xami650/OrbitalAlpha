package org.ulpgc.dacd.weatherfeeder;

import org.ulpgc.dacd.weatherfeeder.config.ConfigLoader;
import org.ulpgc.dacd.weatherfeeder.controller.ClimateController;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.NasaPowerClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.ActiveMqEventPublisher;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.WeatherConfig;

public class ClimateMain {

    public static void main(String[] args) {
        WeatherConfig config = new ConfigLoader().load();

        ProducersInfo producersInfo = new ProducersInfo(config.producersFilePath());
        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder(producersInfo);
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher();

        ClimateController controller = new ClimateController(feeder, producersInfo, publisher, config);
        controller.start();
    }
}