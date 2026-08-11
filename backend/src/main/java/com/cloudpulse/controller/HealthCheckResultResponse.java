package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;

import java.time.Instant;
import java.util.UUID;

/** API view of one persisted health-check result. */
public record HealthCheckResultResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        Instant checkedAt,
        boolean reachable,
        int httpStatus,
        long responseTimeMs,
        String rawMessage,
        ServiceStatus evaluatedStatus) {

    public static HealthCheckResultResponse from(HealthCheckResult result) {
        MonitoredService service = result.getService();
        return new HealthCheckResultResponse(
                result.getId(),
                service != null ? service.getId() : null,
                service != null ? service.getName() : null,
                result.getCheckedAt(),
                result.isReachable(),
                result.getHttpStatus(),
                result.getResponseTimeMs(),
                result.getRawMessage(),
                result.getEvaluatedStatus());
    }
}
