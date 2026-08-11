package com.cloudpulse.monitor.http;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Shared safe-fetch path for both one-off probes and scheduled monitoring.
 * Redirects are followed manually so every hop is validated before a request is sent.
 */
@Component
public class SafeHttpFetcher {

    private final SafeUrlValidator urlValidator;
    private final HttpTransport transport;
    private final HttpMonitoringProperties properties;

    public SafeHttpFetcher(
            SafeUrlValidator urlValidator,
            HttpTransport transport,
            HttpMonitoringProperties properties) {
        this.urlValidator = urlValidator;
        this.transport = transport;
        this.properties = properties;
    }

    public HttpObservation fetch(String rawUrl) {
        URI requestedUri = urlValidator.validate(rawUrl);
        URI currentUri = requestedUri;
        int redirects = 0;
        long startedAt = System.nanoTime();

        while (true) {
            final HttpTransportResponse response;
            try {
                response = transport.execute(currentUri);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return failure(requestedUri, currentUri, startedAt, redirects,
                        "Request was interrupted");
            } catch (IOException exception) {
                return failure(requestedUri, currentUri, startedAt, redirects,
                        "Request failed: " + exception.getClass().getSimpleName());
            }

            if (isRedirect(response.statusCode()) && response.redirectLocation() != null) {
                if (redirects >= properties.getMaxRedirects()) {
                    return failure(requestedUri, currentUri, startedAt, redirects,
                            "Redirect limit exceeded");
                }

                final URI nextUri;
                try {
                    nextUri = currentUri.resolve(response.redirectLocation());
                } catch (IllegalArgumentException exception) {
                    return failure(requestedUri, currentUri, startedAt, redirects,
                            "Endpoint returned an invalid redirect");
                }
                currentUri = urlValidator.validate(nextUri);
                redirects++;
                continue;
            }

            return HttpObservation.success(
                    requestedUri,
                    currentUri,
                    response.statusCode(),
                    elapsedMs(startedAt),
                    response.body(),
                    response.bodyTruncated(),
                    redirects
            );
        }
    }

    private HttpObservation failure(
            URI requestedUri,
            URI finalUri,
            long startedAt,
            int redirects,
            String message) {
        return HttpObservation.failure(
                requestedUri,
                finalUri,
                elapsedMs(startedAt),
                redirects,
                message
        );
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }
}
