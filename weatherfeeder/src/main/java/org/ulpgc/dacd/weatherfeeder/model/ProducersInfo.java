package org.ulpgc.dacd.weatherfeeder.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProducersInfo {

    private final Map<String, Producer> producers;

    public ProducersInfo() {
        this.producers = createProducers();
    }

    public Producer getById(String id) {
        return producers.get(id);
    }

    public List<String> getAllIds() {
        return List.copyOf(producers.keySet());
    }

    public boolean exists(String id) {
        return producers.containsKey(id);
    }

    public Map<String, Producer> getAll() {
        return Map.copyOf(producers);
    }

    private Map<String, Producer> createProducers() {
        Map<String, Producer> map = new LinkedHashMap<>();

        // WHEAT
        map.put("WHEAT_1", new Producer("WHEAT_1", "Beauce - France", "WHEAT", 48.10, 1.75));
        map.put("WHEAT_2", new Producer("WHEAT_2", "Henan - China", "WHEAT", 34.75, 113.62));
        map.put("WHEAT_3", new Producer("WHEAT_3", "Punjab - India", "WHEAT", 31.15, 75.34));
        map.put("WHEAT_4", new Producer("WHEAT_4", "Krasnodar Krai - Russia", "WHEAT", 45.04, 38.98));
        map.put("WHEAT_5", new Producer("WHEAT_5", "Kansas - USA", "WHEAT", 38.50, -98.00));

        // CORN
        map.put("CORN_1", new Producer("CORN_1", "Iowa - USA", "CORN", 42.03, -93.58));
        map.put("CORN_2", new Producer("CORN_2", "Jilin - China", "CORN", 43.90, 125.32));
        map.put("CORN_3", new Producer("CORN_3", "Mato Grosso - Brazil", "CORN", -12.64, -55.42));
        map.put("CORN_4", new Producer("CORN_4", "Cordoba - Argentina", "CORN", -31.42, -64.19));
        map.put("CORN_5", new Producer("CORN_5", "Baragan Plain - Romania", "CORN", 44.50, 27.50));

        // SOY BEANS
        map.put("SOY_1", new Producer("SOY_1", "Mato Grosso - Brazil", "SOY_BEANS", -12.64, -55.42));
        map.put("SOY_2", new Producer("SOY_2", "Illinois - USA", "SOY_BEANS", 40.00, -89.20));
        map.put("SOY_3", new Producer("SOY_3", "Cordoba - Argentina", "SOY_BEANS", -31.42, -64.19));
        map.put("SOY_4", new Producer("SOY_4", "Heilongjiang - China", "SOY_BEANS", 47.86, 127.76));
        map.put("SOY_5", new Producer("SOY_5", "Alto Parana - Paraguay", "SOY_BEANS", -25.45, -54.90));

        // COFFEE
        map.put("COFFEE_1", new Producer("COFFEE_1", "Minas Gerais - Brazil", "COFFEE", -18.51, -44.56));
        map.put("COFFEE_2", new Producer("COFFEE_2", "Dak Lak - Vietnam", "COFFEE", 12.71, 108.24));
        map.put("COFFEE_3", new Producer("COFFEE_3", "Antioquia - Colombia", "COFFEE", 6.55, -75.57));
        map.put("COFFEE_4", new Producer("COFFEE_4", "Lampung - Indonesia", "COFFEE", -4.56, 105.41));
        map.put("COFFEE_5", new Producer("COFFEE_5", "Oromia - Ethiopia", "COFFEE", 7.55, 40.63));

        // NATURAL GAS
        map.put("NATGAS_1", new Producer("NATGAS_1", "Texas - USA", "NATURAL_GAS", 31.00, -99.00));
        map.put("NATGAS_2", new Producer("NATGAS_2", "Pennsylvania - USA", "NATURAL_GAS", 41.20, -77.19));
        map.put("NATGAS_3", new Producer("NATGAS_3", "Louisiana - USA", "NATURAL_GAS", 31.00, -92.00));
        map.put("NATGAS_4", new Producer("NATGAS_4", "West Virginia - USA", "NATURAL_GAS", 38.60, -80.45));
        map.put("NATGAS_5", new Producer("NATGAS_5", "Oklahoma - USA", "NATURAL_GAS", 35.59, -97.49));

        return map;
    }

    public record Producer(
            String id,
            String name,
            String commodityType,
            double latitude,
            double longitude
    ) {
    }
}
