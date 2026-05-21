package org.ulpgc.dacd.weatherfeeder.controller.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.NasaPowerApiConfig;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class NasaPowerClimateFeeder implements ClimateFeeder {

    private static final Logger logger = LoggerFactory.getLogger(NasaPowerClimateFeeder.class);

    private final OkHttpClient client;
    private final NasaPowerClimateParser parser;
    private final NasaPowerApiConfig apiConfig;

    public NasaPowerClimateFeeder(OkHttpClient client, NasaPowerApiConfig apiConfig, NasaPowerClimateParser parser) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null.");
        }
        if (apiConfig == null) {
            throw new IllegalArgumentException("apiConfig must not be null.");
        }
        if (parser == null) {
            throw new IllegalArgumentException("parser must not be null.");
        }
        this.client = client;
        this.apiConfig = apiConfig;
        this.parser = parser;
    }

    @Override
    public List<WeatherEvent> fetch(Producer producer, DateRange range) {
        if (producer == null) {
            logger.error("Producer is null when requesting climate data.");
            return Collections.emptyList();
        }

        String url = buildUrl(producer, range);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, producer, range);
        } catch (IOException e) {
            logger.error("Connection failure with NASA POWER for {} in {}.", producer.id(), range, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Unexpected error processing {} in {}.", producer.id(), range, e);
            return Collections.emptyList();
        }
    }

    private String buildUrl(Producer producer, DateRange range) {
        return String.format(
                apiConfig.urlTemplate(),
                producer.longitude(),
                producer.latitude(),
                range.startAsApiDate(),
                range.endAsApiDate()
        );
    }

    private List<WeatherEvent> handleResponse(Response response, Producer producer, DateRange range) throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            logger.error("HTTP {} when querying NASA POWER for {} in {}.", response.code(), producer.id(), range);
            return Collections.emptyList();
        }
        return parser.parse(response.body().string(), producer);
    }
}