package org.ulpgc.dacd.weatherfeeder.controller.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.Instant;

public class GsonEventSerializer implements EventSerializer {

    private final Gson gson;

    public GsonEventSerializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(
                        Instant.class,
                        (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString())
                )
                .create();
    }

    @Override
    public String serialize(Object event) {
        return gson.toJson(event);
    }
}