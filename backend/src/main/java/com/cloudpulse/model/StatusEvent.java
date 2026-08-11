package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "status_event")
public class StatusEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private MonitoredService service;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private ServiceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ServiceStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected StatusEvent() {
    }

    public StatusEvent(MonitoredService service, ServiceStatus previousStatus, ServiceStatus newStatus, String reason) {
        this.service = service;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public MonitoredService getService() { return service; }
    public ServiceStatus getPreviousStatus() { return previousStatus; }
    public ServiceStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getOccurredAt() { return occurredAt; }
}