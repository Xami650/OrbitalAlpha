package org.ulpgc.dacd.weatherfeeder.controller.feeder;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.dacd.weatherfeeder.controller.feeder.parser.NasaPowerClimateParser;
import org.ulpgc.dacd.weatherfeeder.model.DateRange;
import org.ulpgc.dacd.weatherfeeder.model.NasaPowerApiConfig;
import org.ulpgc.dacd.weatherfeeder.model.ProducersInfo.Producer;
import org.ulpgc.dacd.weatherfeeder.model.events.WeatherEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NasaPowerClimateFeederTest {

    private OkHttpClient client;
    private NasaPowerClimateParser parser;
    private NasaPowerApiConfig apiConfig;
    private NasaPowerClimateFeeder feeder;

    private final Producer producer = new Producer("P1", "Producer 1", "WHEAT", 10.0, 20.0);
    private final DateRange range = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7));

    @BeforeEach
    void setUp() {
        client = mock(OkHttpClient.class);
        parser = mock(NasaPowerClimateParser.class);
        apiConfig = new NasaPowerApiConfig("http://nasa.api/%f/%f/%s/%s", 0);
        feeder = new NasaPowerClimateFeeder(client, apiConfig, parser);
    }

    @Test
    void fetch_whenResponseIsSuccessful_returnsParsedEvents() throws IOException {
        String jsonResponse = "{\"data\": \"some json\"}";
        Response response = createMockResponse(200, jsonResponse);
        Call call = mock(Call.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        List<WeatherEvent> expectedEvents = List.of(mock(WeatherEvent.class));
        when(parser.parse(jsonResponse, producer)).thenReturn(expectedEvents);

        List<WeatherEvent> result = feeder.fetch(producer, range);

        assertThat(result).isEqualTo(expectedEvents);
        verify(parser).parse(jsonResponse, producer);
    }

    @Test
    void fetch_whenResponseIsError_returnsEmptyList() throws IOException {
        Response response = createMockResponse(500, "");
        Call call = mock(Call.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        List<WeatherEvent> result = feeder.fetch(producer, range);

        assertThat(result).isEmpty();
        verify(parser, never()).parse(anyString(), any());
    }

    @Test
    void fetch_whenIoExceptionOccurs_returnsEmptyList() throws IOException {
        Call call = mock(Call.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("Network error"));

        List<WeatherEvent> result = feeder.fetch(producer, range);

        assertThat(result).isEmpty();
    }

    private Response createMockResponse(int code, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("http://nasa.api").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Message")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();
    }
}