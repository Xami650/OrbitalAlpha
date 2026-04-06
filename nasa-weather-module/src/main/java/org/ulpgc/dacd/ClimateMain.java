package org.ulpgc.dacd;

public class ClimateMain {
    public static void main(String[] args) {
        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder();
        SqliteClimateStore store = new SqliteClimateStore();
        store.initialize();

        ClimateController controller = new ClimateController(feeder, store);
        controller.start();
    }
}