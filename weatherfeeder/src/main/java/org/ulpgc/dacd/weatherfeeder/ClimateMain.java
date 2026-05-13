package org.ulpgc.dacd.weatherfeeder;

import org.ulpgc.dacd.weatherfeeder.controller.ClimateController;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.NasaPowerClimateFeeder;
import org.ulpgc.dacd.weatherfeeder.controller.publisher.ActiveMqEventPublisher;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ClimateMain {

    private static final Path DEFAULT_PRODUCERS_FILE = Paths.get("config", "producers.csv");
    private static final int DEFAULT_WEEKS_TO_FETCH = 4;

    public static void main(String[] args) {
        AppConfig config = readConfig(args);
        ClimateController controller = createController(config);
        controller.start();
    }

    private static ClimateController createController(AppConfig config) {
        ProducersInfo producersInfo = new ProducersInfo(config.producersFilePath());

        NasaPowerClimateFeeder feeder = new NasaPowerClimateFeeder(
                producersInfo,
                config.weeksToFetch()
        );

        ActiveMqEventPublisher publisher = new ActiveMqEventPublisher();

        return new ClimateController(feeder, producersInfo, publisher);
    }

    private static AppConfig readConfig(String[] args) {
        if (args.length == 0) {
            return new AppConfig(DEFAULT_PRODUCERS_FILE, DEFAULT_WEEKS_TO_FETCH);
        }

        if (args.length == 1 && isInteger(args[0])) {
            return new AppConfig(DEFAULT_PRODUCERS_FILE, parseWeeksToFetch(args[0]));
        }

        if (args.length == 1) {
            return new AppConfig(Paths.get(args[0]), DEFAULT_WEEKS_TO_FETCH);
        }

        if (args.length == 2) {
            return new AppConfig(Paths.get(args[0]), parseWeeksToFetch(args[1]));
        }

        throw new IllegalArgumentException(
                "Uso incorrecto. Formatos válidos: " +
                        "sin argumentos, <weeks>, <producersFile>, o <producersFile> <weeks>."
        );
    }

    private static int parseWeeksToFetch(String value) {
        int weeks = Integer.parseInt(value);

        if (weeks <= 0) {
            throw new IllegalArgumentException("El número de semanas debe ser mayor que 0.");
        }

        return weeks;
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private record AppConfig(
            Path producersFilePath,
            int weeksToFetch
    ) {
    }
}