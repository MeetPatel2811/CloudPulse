package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A scheduled span of time during which a service's alerts are suppressed. */
@Entity
@Table(name = "maintenance_window")
public class MaintenanceWindow {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private MonitoredService service;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MaintenanceWindow() {
    }

    public MaintenanceWindow(MonitoredService service, Instant startsAt, Instant endsAt, String reason) {
        this.service = service;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public MonitoredService getService() { return service; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
