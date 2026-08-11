package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Final Adapter fallback for ordinary HTML pages and unstructured HTTP endpoints.
 * Structured JSON adapters run first; this adapter intentionally supports everything.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GenericHttpAdapter implements HealthResponseAdapter {

    @Override
    public HealthCheckResult adapt(String rawResponse, int httpStatus, long responseTimeMs) {
        boolean reachable = httpStatus >= 200 && httpStatus < 400;
        String message = "Generic HTTP response returned status " + httpStatus;
        return new HealthCheckResult(reachable, httpStatus, responseTimeMs, message);
    }

    @Override
    public boolean supports(String rawResponse) {
        return true;
    }

    @Override
    public HttpResponseFormat format() {
        return HttpResponseFormat.GENERIC_HTTP;
    }
}
