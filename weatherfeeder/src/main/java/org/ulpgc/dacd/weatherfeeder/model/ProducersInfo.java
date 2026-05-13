package org.ulpgc.dacd.weatherfeeder.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProducersInfo {

    private static final String FIELD_SEPARATOR = ";";
    private static final int EXPECTED_FIELDS = 5;

    private final Map<String, Producer> producers;

    public ProducersInfo(Path producersFilePath) {
        this.producers = loadProducers(producersFilePath);
    }

    public Producer getById(String id) {
        return producers.get(id);
    }

    public List<String> getAllIds() {
        return List.copyOf(producers.keySet());
    }

    private Map<String, Producer> loadProducers(Path producersFilePath) {
        validateFilePath(producersFilePath);

        try {
            List<String> lines = Files.readAllLines(producersFilePath, StandardCharsets.UTF_8);

            Map<String, Producer> loadedProducers = IntStream.range(0, lines.size())
                    .mapToObj(index -> new NumberedLine(index + 1, cleanLine(lines.get(index))))
                    .filter(numberedLine -> !shouldIgnoreLine(numberedLine.content()))
                    .map(numberedLine -> parseProducer(numberedLine.content(), numberedLine.number()))
                    .collect(Collectors.toMap(
                            Producer::id,
                            Function.identity(),
                            (existing, duplicated) -> {
                                throw new IllegalArgumentException("Productor duplicado: " + duplicated.id());
                            },
                            LinkedHashMap::new
                    ));

            if (loadedProducers.isEmpty()) {
                throw new IllegalArgumentException("El archivo de productores está vacío: " + producersFilePath);
            }

            return loadedProducers;

        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de productores: " + producersFilePath, e);
        }
    }

    private void validateFilePath(Path producersFilePath) {
        if (producersFilePath == null) {
            throw new IllegalArgumentException("La ruta del archivo de productores no puede ser null.");
        }

        if (!Files.exists(producersFilePath)) {
            throw new IllegalArgumentException("No existe el archivo de productores: " + producersFilePath);
        }

        if (!Files.isRegularFile(producersFilePath)) {
            throw new IllegalArgumentException("La ruta no corresponde a un archivo válido: " + producersFilePath);
        }
    }

    private String cleanLine(String line) {
        return line.replace("\uFEFF", "").trim();
    }

    private boolean shouldIgnoreLine(String line) {
        return line.isBlank() || line.startsWith("#");
    }

    private Producer parseProducer(String line, int lineNumber) {
        String[] fields = line.split(Pattern.quote(FIELD_SEPARATOR), -1);

        if (fields.length != EXPECTED_FIELDS) {
            throw new IllegalArgumentException(
                    "Formato inválido en línea " + lineNumber +
                            ". Formato esperado: id;name;commodityType;latitude;longitude"
            );
        }

        String id = fields[0].trim();
        String name = fields[1].trim();
        String commodityType = fields[2].trim();
        double latitude = parseDouble(fields[3].trim(), "latitude", lineNumber);
        double longitude = parseDouble(fields[4].trim(), "longitude", lineNumber);

        validateRequiredField(id, "id", lineNumber);
        validateRequiredField(name, "name", lineNumber);
        validateRequiredField(commodityType, "commodityType", lineNumber);
        validateCoordinates(latitude, longitude, lineNumber);

        return new Producer(id, name, commodityType, latitude, longitude);
    }

    private double parseDouble(String value, String fieldName, int lineNumber) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El campo " + fieldName + " no es un número válido en la línea " + lineNumber + ": " + value,
                    e
            );
        }
    }

    private void validateRequiredField(String value, String fieldName, int lineNumber) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "El campo " + fieldName + " no puede estar vacío en la línea " + lineNumber
            );
        }
    }

    private void validateCoordinates(double latitude, double longitude, int lineNumber) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitud fuera de rango en línea " + lineNumber + ": " + latitude);
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitud fuera de rango en línea " + lineNumber + ": " + longitude);
        }
    }

    private record NumberedLine(
            int number,
            String content
    ) {
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