package com.cloudpulse.monitor.http;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** JDK HttpClient transport with automatic redirects disabled and response bodies capped. */
@Component
public class JdkHttpTransport implements HttpTransport {

    private final HttpMonitoringProperties properties;
    private final HttpClient httpClient;

    public JdkHttpTransport(HttpMonitoringProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public HttpTransportResponse execute(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(properties.getRequestTimeout())
                .header("User-Agent", properties.getUserAgent())
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        String location = response.headers().firstValue("Location").orElse(null);

        try (InputStream input = response.body()) {
            if (isRedirect(response.statusCode())) {
                return new HttpTransportResponse(response.statusCode(), "", false, location);
            }

            int limit = properties.getMaxResponseBytes();
            byte[] bytes = input.readNBytes(limit + 1);
            boolean truncated = bytes.length > limit;
            int bodyLength = Math.min(bytes.length, limit);
            String body = new String(bytes, 0, bodyLength, StandardCharsets.UTF_8);
            return new HttpTransportResponse(response.statusCode(), body, truncated, null);
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }
}
