package com.cloudpulse.controller;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        Severity severity,
        AlertStatus status,
        String message,
        Instant createdAt,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant resolvedAt) {

    public static AlertResponse from(Alert alert) {
        MonitoredService service = alert.getService();
        return new AlertResponse(
                alert.getId(),
                service != null ? service.getId() : null,
                service != null ? service.getName() : null,
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getAcknowledgedBy(),
                alert.getResolvedAt());
    }
}
