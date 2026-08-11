package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "monitored_service")
public class MonitoredService {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "health_url", nullable = false)
    private String healthUrl;

    @Column(name = "service_group", length = 80)
    private String serviceGroup;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "monitored_service_tag",
            joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "tag", nullable = false, length = 30)
    private Set<String> tags = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "monitor_type", nullable = false)
    private MonitorType monitorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private ServiceStatus currentStatus = ServiceStatus.UNKNOWN;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "check_interval_seconds", nullable = false)
    private long checkIntervalSeconds = 15;

    /** Required when monitorType is KEYWORD: text that must appear in the response body. */
    @Column(name = "expected_keyword")
    private String expectedKeyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_evaluation_strategy", nullable = false)
    private EvaluationPolicy activeEvaluationStrategy = EvaluationPolicy.NORMAL;

    @Column(name = "latency_threshold_ms")
    private Long latencyThresholdMs;

    @Column(name = "availability_slo_percent")
    private Double availabilitySloPercent = 99.0;

    // Nullable at the database level so Hibernate can add these columns safely to
    // existing H2/Postgres installations; the domain getters always expose defaults.
    @Column(name = "alert_failure_threshold")
    private Integer alertFailureThreshold = 1;

    @Column(name = "consecutive_failure_count")
    private Integer consecutiveFailureCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    protected MonitoredService() {
    }

    public MonitoredService(String name, String healthUrl, MonitorType monitorType) {
        this.name = name;
        this.healthUrl = healthUrl;
        this.monitorType = monitorType;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHealthUrl() { return healthUrl; }
    public void setHealthUrl(String healthUrl) { this.healthUrl = healthUrl; }
    public String getServiceGroup() { return serviceGroup; }
    public void setServiceGroup(String serviceGroup) { this.serviceGroup = serviceGroup; }
    public Set<String> getTags() { return tags; }
    public void setTags(Collection<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
    public MonitorType getMonitorType() { return monitorType; }
    public void setMonitorType(MonitorType monitorType) { this.monitorType = monitorType; }
    public ServiceStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(ServiceStatus currentStatus) { this.currentStatus = currentStatus; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(long checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }
    public String getExpectedKeyword() { return expectedKeyword; }
    public void setExpectedKeyword(String expectedKeyword) { this.expectedKeyword = expectedKeyword; }
    public EvaluationPolicy getActiveEvaluationStrategy() { return activeEvaluationStrategy; }
    public void setActiveEvaluationStrategy(EvaluationPolicy activeEvaluationStrategy) { this.activeEvaluationStrategy = activeEvaluationStrategy; }
    public Long getLatencyThresholdMs() { return latencyThresholdMs; }
    public void setLatencyThresholdMs(Long latencyThresholdMs) { this.latencyThresholdMs = latencyThresholdMs; }
    public double getAvailabilitySloPercent() {
        return availabilitySloPercent == null ? 99.0 : availabilitySloPercent;
    }
    public void setAvailabilitySloPercent(double availabilitySloPercent) {
        this.availabilitySloPercent = availabilitySloPercent;
    }
    public int getAlertFailureThreshold() { return alertFailureThreshold == null ? 1 : alertFailureThreshold; }
    public void setAlertFailureThreshold(int alertFailureThreshold) { this.alertFailureThreshold = alertFailureThreshold; }
    public int getConsecutiveFailureCount() { return consecutiveFailureCount == null ? 0 : consecutiveFailureCount; }
    public int recordUnhealthyCheck() {
        consecutiveFailureCount = getConsecutiveFailureCount() + 1;
        return consecutiveFailureCount;
    }
    public void resetConsecutiveFailureCount() { consecutiveFailureCount = 0; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
}
