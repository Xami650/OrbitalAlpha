package org.ulpgc.dacd.weatherfeeder.view;

import org.ulpgc.dacd.weatherfeeder.controller.ClimateController;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.feeders.NasaPowerClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.model.storers.SqliteClimateDatabaseInitializer;
import org.ulpgc.dacd.weatherfeeder.model.storers.SqliteClimateStore;

public class ClimateMain {

    public static void main(String[] args) {
        ProducersInfo producersInfo = new ProducersInfo();
        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder(producersInfo);
        SqliteClimateStore store = new SqliteClimateStore();
        SqliteClimateDatabaseInitializer initializer = new SqliteClimateDatabaseInitializer();

        initializer.initialize();

        ClimateController controller = new ClimateController(feeder, store, producersInfo);
        controller.start();
    }
}