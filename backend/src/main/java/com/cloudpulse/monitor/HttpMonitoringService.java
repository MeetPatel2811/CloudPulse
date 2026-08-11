package com.cloudpulse.monitor;

import com.cloudpulse.adapter.AdaptedHealthResponse;
import com.cloudpulse.adapter.HealthResponseAdapterRegistry;
import com.cloudpulse.adapter.HttpResponseFormat;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.monitor.http.HttpObservation;
import com.cloudpulse.monitor.http.SafeHttpFetcher;
import org.springframework.stereotype.Service;

/** Shared inspection service used by both scheduled monitoring and URL probes. */
@Service
public class HttpMonitoringService {

    private static final int MAX_PERSISTED_MESSAGE_LENGTH = 2_000;

    private final SafeHttpFetcher fetcher;
    private final HealthResponseAdapterRegistry adapterRegistry;

    public HttpMonitoringService(
            SafeHttpFetcher fetcher,
            HealthResponseAdapterRegistry adapterRegistry) {
        this.fetcher = fetcher;
        this.adapterRegistry = adapterRegistry;
    }

    public HttpMonitoringInspection inspect(String url) {
        HttpObservation observation = fetcher.fetch(url);
        if (!observation.completed()) {
            HealthCheckResult failed = new HealthCheckResult(
                    false,
                    0,
                    observation.responseTimeMs(),
                    observation.failureMessage()
            );
            return new HttpMonitoringInspection(
                    failed,
                    HttpResponseFormat.GENERIC_HTTP,
                    observation.finalUri(),
                    observation.redirectCount(),
                    false
            );
        }

        AdaptedHealthResponse adapted = adapterRegistry.adapt(
                observation.body(),
                observation.httpStatus(),
                observation.responseTimeMs()
        );
        truncatePersistedMessage(adapted.result());

        return new HttpMonitoringInspection(
                adapted.result(),
                adapted.format(),
                observation.finalUri(),
                observation.redirectCount(),
                observation.bodyTruncated()
        );
    }

    private void truncatePersistedMessage(HealthCheckResult result) {
        String message = result.getRawMessage();
        if (message != null && message.length() > MAX_PERSISTED_MESSAGE_LENGTH) {
            result.setRawMessage(message.substring(0, MAX_PERSISTED_MESSAGE_LENGTH - 3) + "...");
        }
    }
}
