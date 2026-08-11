package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.monitor.HttpMonitoringInspection;
import com.cloudpulse.monitor.HttpMonitoringService;
import org.springframework.stereotype.Service;

/** Runs a safe, non-persisting preview through the same path as scheduled checks. */
@Service
public class ProbeService {

    private final HttpMonitoringService monitoringService;

    public ProbeService(HttpMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    public ProbeServiceResponse probe(String url) {
        HttpMonitoringInspection inspection = monitoringService.inspect(url);
        HealthCheckResult result = inspection.result();

        return new ProbeServiceResponse(
                result.isReachable(),
                result.getHttpStatus(),
                result.getResponseTimeMs(),
                inspection.responseFormat(),
                inspection.finalUri().toString(),
                inspection.redirectCount(),
                inspection.bodyTruncated(),
                messageFor(result)
        );
    }

    private String messageFor(HealthCheckResult result) {
        if (result.isReachable()) {
            return "Connection successful";
        }
        if (result.getHttpStatus() >= 400) {
            return "Endpoint returned HTTP " + result.getHttpStatus();
        }
        if (result.getHttpStatus() > 0) {
            return "Endpoint reported an unhealthy status";
        }
        return result.getRawMessage();
    }
}
