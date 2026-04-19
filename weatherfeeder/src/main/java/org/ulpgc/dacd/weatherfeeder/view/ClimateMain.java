package org.ulpgc.dacd.weatherfeeder.view;

import org.ulpgc.dacd.weatherfeeder.controller.ClimateController;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.feeders.NasaPowerClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.publisher.ActiveMqEventPublisher;

public class ClimateMain {

    public static void main(String[] args) {
        ProducersInfo producersInfo = new ProducersInfo();
        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder(producersInfo);
        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher();

        ClimateController controller = new ClimateController(feeder, producersInfo, publisher);
        controller.start();
    }
}