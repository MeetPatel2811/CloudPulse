package com.cloudpulse.controller;

import com.cloudpulse.model.MaintenanceWindow;
import com.cloudpulse.model.MonitoredService;

import java.time.Instant;
import java.util.UUID;

/** API view of a scheduled maintenance window, with its status relative to now. */
public record MaintenanceWindowResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        Instant startsAt,
        Instant endsAt,
        String reason,
        MaintenanceWindowStatus status) {

    public enum MaintenanceWindowStatus { SCHEDULED, ACTIVE, ENDED }

    public static MaintenanceWindowResponse from(MaintenanceWindow window) {
        MonitoredService service = window.getService();
        Instant now = Instant.now();
        MaintenanceWindowStatus status = now.isBefore(window.getStartsAt())
                ? MaintenanceWindowStatus.SCHEDULED
                : now.isBefore(window.getEndsAt())
                        ? MaintenanceWindowStatus.ACTIVE
                        : MaintenanceWindowStatus.ENDED;

        return new MaintenanceWindowResponse(
                window.getId(),
                service != null ? service.getId() : null,
                service != null ? service.getName() : null,
                window.getStartsAt(),
                window.getEndsAt(),
                window.getReason(),
                status);
    }
}
