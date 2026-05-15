package org.ulpgc.dacd.weatherfeeder.controller.feeder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.WeatherEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class NasaPowerClimateFeeder implements ClimateFeeder {

    private static final Logger logger = LoggerFactory.getLogger(NasaPowerClimateFeeder.class);

    private static final String API_URL_TEMPLATE =
            "https://power.larc.nasa.gov/api/temporal/daily/point" +
                    "?parameters=PRECTOTCORR,GWETROOT,T2M_MAX,T2M_MIN" +
                    "&community=AG" +
                    "&longitude=%s" +
                    "&latitude=%s" +
                    "&start=%s" +
                    "&end=%s" +
                    "&format=JSON";

    private final OkHttpClient client;
    private final ProducersInfo producersInfo;
    private final NasaPowerClimateParser parser;

    public NasaPowerClimateFeeder(ProducersInfo producersInfo) {
        this.client = new OkHttpClient();
        this.producersInfo = producersInfo;
        this.parser = new NasaPowerClimateParser();
    }

    @Override
    public List<WeatherEvent> fetch(String producerId, DateRange range) {
        Producer producer = producersInfo.getById(producerId);

        if (producer == null) {
            logger.error("Productor o región no reconocido: {}", producerId);
            return Collections.emptyList();
        }

        String url = buildUrl(producer, range.startAsApiDate(), range.endAsApiDate());
        Request request = buildRequest(url);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, producer, producerId, range);
        } catch (IOException e) {
            logger.error("Fallo de conexión con NASA POWER para {} en {}.", producerId, range, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error inesperado procesando {} en {}.", producerId, range, e);
            return Collections.emptyList();
        }
    }

    private String buildUrl(Producer producer, String startDate, String endDate) {
        return String.format(
                API_URL_TEMPLATE,
                producer.longitude(),
                producer.latitude(),
                startDate,
                endDate
        );
    }

    private Request buildRequest(String url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }

    private List<WeatherEvent> handleResponse(
            Response response,
            Producer producer,
            String producerId,
            DateRange range
    ) throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            logger.error("HTTP {} al consultar NASA POWER para {} en {}.", response.code(), producerId, range);
            return Collections.emptyList();
        }

        String jsonResponse = response.body().string();
        return parser.parse(jsonResponse, producer, producerId);
    }
}