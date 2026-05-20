package org.ulpgc.dacd.weatherfeeder.config;

import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;

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

public class ProducersCsvLoader {

    private static final String FIELD_SEPARATOR = ";";
    private static final int EXPECTED_FIELDS = 5;
    private static final String BOM = "﻿";
    private static final String COMMENT_PREFIX = "#";

    public Map<String, Producer> load(Path producersFilePath) {
        validateFilePath(producersFilePath);

        try {
            List<String> lines = Files.readAllLines(producersFilePath, StandardCharsets.UTF_8);

            Map<String, Producer> loaded = IntStream.range(0, lines.size())
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

            if (loaded.isEmpty()) {
                throw new IllegalArgumentException("El archivo de productores esta vacio: " + producersFilePath);
            }

            return loaded;

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
            throw new IllegalArgumentException("La ruta no corresponde a un archivo valido: " + producersFilePath);
        }
    }

    private String cleanLine(String line) {
        return line.replace(BOM, "").trim();
    }

    private boolean shouldIgnoreLine(String line) {
        return line.isBlank() || line.startsWith(COMMENT_PREFIX);
    }

    private Producer parseProducer(String line, int lineNumber) {
        String[] fields = line.split(Pattern.quote(FIELD_SEPARATOR), -1);

        if (fields.length != EXPECTED_FIELDS) {
            throw new IllegalArgumentException(
                    "Formato invalido en linea " + lineNumber +
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
                    "El campo " + fieldName + " no es un numero valido en la linea " + lineNumber + ": " + value,
                    e
            );
        }
    }

    private void validateRequiredField(String value, String fieldName, int lineNumber) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "El campo " + fieldName + " no puede estar vacio en la linea " + lineNumber
            );
        }
    }

    private void validateCoordinates(double latitude, double longitude, int lineNumber) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitud fuera de rango en linea " + lineNumber + ": " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitud fuera de rango en linea " + lineNumber + ": " + longitude);
        }
    }

    private record NumberedLine(int number, String content) {
    }
}