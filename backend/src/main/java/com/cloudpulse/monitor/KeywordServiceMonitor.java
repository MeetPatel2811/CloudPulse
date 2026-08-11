package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.http.HttpObservation;
import com.cloudpulse.monitor.http.SafeHttpFetcher;
import org.springframework.stereotype.Component;

/** Fetches the page and reports it unreachable if the service's expected keyword is missing. */
@Component
public class KeywordServiceMonitor implements ServiceMonitor {

    private final SafeHttpFetcher fetcher;

    public KeywordServiceMonitor(SafeHttpFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        HttpObservation observation = fetcher.fetch(service.getHealthUrl());
        if (!observation.completed()) {
            return new HealthCheckResult(false, 0, observation.responseTimeMs(), observation.failureMessage());
        }

        String keyword = service.getExpectedKeyword();
        boolean found = keyword != null && !keyword.isBlank()
                && observation.body() != null && observation.body().contains(keyword);

        String message = found
                ? "Keyword '" + keyword + "' found in response"
                : "Keyword '" + keyword + "' not found in response";

        return new HealthCheckResult(found, observation.httpStatus(), observation.responseTimeMs(), message);
    }
}
