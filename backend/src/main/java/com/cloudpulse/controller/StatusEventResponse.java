package com.cloudpulse.controller;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;

import java.time.Instant;
import java.util.UUID;

// API view of a StatusEvent: one recorded lifecycle transition for a service.
public record StatusEventResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        ServiceStatus previousStatus,
        ServiceStatus newStatus,
        String reason,
        Instant occurredAt) {

    public static StatusEventResponse from(StatusEvent event) {
        MonitoredService service = event.getService();
        return new StatusEventResponse(
                event.getId(),
                service != null ? service.getId() : null,
                service != null ? service.getName() : null,
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getOccurredAt());
    }
}
