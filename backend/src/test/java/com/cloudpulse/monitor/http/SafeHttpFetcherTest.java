package com.cloudpulse.monitor.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeHttpFetcherTest {

    @Test
    void followsBoundedRedirectAndReturnsFinalObservation() {
        HttpTransport transport = uri -> uri.getPath().equals("/start")
                ? new HttpTransportResponse(302, "", false, "/final")
                : new HttpTransportResponse(200, "<html>ok</html>", false, null);
        SafeHttpFetcher fetcher = fetcher(transport, 3);

        HttpObservation result = fetcher.fetch("https://public.example/start");

        assertTrue(result.completed());
        assertEquals(200, result.httpStatus());
        assertEquals("https://public.example/final", result.finalUri().toString());
        assertEquals(1, result.redirectCount());
    }

    @Test
    void validatesEveryRedirectDestination() {
        HttpTransport transport = uri -> new HttpTransportResponse(
                302, "", false, "http://169.254.169.254/latest/meta-data");
        SafeHttpFetcher fetcher = fetcher(transport, 3);

        assertThrows(IllegalArgumentException.class,
                () -> fetcher.fetch("https://public.example/start"));
    }

    @Test
    void redirectLimitBecomesFailedObservation() {
        HttpTransport transport = uri ->
                new HttpTransportResponse(302, "", false, "/another");
        SafeHttpFetcher fetcher = fetcher(transport, 1);

        HttpObservation result = fetcher.fetch("https://public.example/start");

        assertFalse(result.completed());
        assertEquals("Redirect limit exceeded", result.failureMessage());
    }

    @Test
    void networkFailureBecomesFailedObservation() {
        HttpTransport transport = uri -> {
            throw new IOException("connection refused");
        };
        SafeHttpFetcher fetcher = fetcher(transport, 3);

        HttpObservation result = fetcher.fetch("https://public.example/start");

        assertFalse(result.completed());
        assertEquals(0, result.httpStatus());
        assertTrue(result.failureMessage().contains("IOException"));
    }

    private SafeHttpFetcher fetcher(HttpTransport transport, int maxRedirects) {
        HttpMonitoringProperties properties = new HttpMonitoringProperties();
        properties.setAllowedHosts(List.of("public.example"));
        properties.setMaxRedirects(maxRedirects);
        return new SafeHttpFetcher(new SafeUrlValidator(properties), transport, properties);
    }
}
