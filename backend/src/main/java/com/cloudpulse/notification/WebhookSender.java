package com.cloudpulse.notification;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Posts a pre-built JSON body to a webhook URL and reports the outcome. Never throws:
// any failure (bad URL, timeout, non-2xx) comes back as an unsuccessful DeliveryResult
// so the notifier can record it without disrupting the alert flow.
@Component
public class WebhookSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_ERROR_LENGTH = 500;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public DeliveryResult send(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            boolean ok = status >= 200 && status < 300;
            return new DeliveryResult(ok, status, ok ? null : truncate("HTTP " + status + ": " + response.body()));
        } catch (Exception e) {
            return new DeliveryResult(false, 0, truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
