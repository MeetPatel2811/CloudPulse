package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_check_result")
public class HealthCheckResult {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private MonitoredService service;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt = Instant.now();

    @Column(nullable = false)
    private boolean reachable;

    @Column(name = "http_status")
    private int httpStatus;

    @Column(name = "response_time_ms")
    private long responseTimeMs;

    @Column(name = "raw_message", length = 2000)
    private String rawMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluated_status")
    private ServiceStatus evaluatedStatus = ServiceStatus.UNKNOWN;

    public HealthCheckResult() {
    }

    public HealthCheckResult(boolean reachable, int httpStatus, long responseTimeMs, String rawMessage) {
        this.reachable = reachable;
        this.httpStatus = httpStatus;
        this.responseTimeMs = responseTimeMs;
        this.rawMessage = rawMessage;
    }

    public UUID getId() { return id; }
    public MonitoredService getService() { return service; }
    public void setService(MonitoredService service) { this.service = service; }
    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }
    public ServiceStatus getEvaluatedStatus() { return evaluatedStatus; }
    public void setEvaluatedStatus(ServiceStatus evaluatedStatus) { this.evaluatedStatus = evaluatedStatus; }
}