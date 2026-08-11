package com.cloudpulse.handler;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.Severity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.MaintenanceWindowRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

// Step 5: turn evaluated health results into threshold-aware alert actions.
//   DOWN     -> raise a CRITICAL alert
//   DEGRADED -> raise a WARNING alert
//   HEALTHY  -> service is back, so resolve its open alerts
// Every result reaches this handler so pending failure streaks can advance and
// prematurely resolved incidents can reopen without producing duplicates.
// A service currently in a maintenance window is skipped entirely: no failure
// streak, alert, or resolution changes until the window ends.
public class AlertCreationHandler extends ResultHandler {

    private static final Logger log = LoggerFactory.getLogger(AlertCreationHandler.class);

    private final AlertRepository alertRepository;
    private final MonitoredServiceRepository monitoredServiceRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;

    public AlertCreationHandler(
            AlertRepository alertRepository,
            MonitoredServiceRepository monitoredServiceRepository,
            MaintenanceWindowRepository maintenanceWindowRepository) {
        this.alertRepository = alertRepository;
        this.monitoredServiceRepository = monitoredServiceRepository;
        this.maintenanceWindowRepository = maintenanceWindowRepository;
    }

    @Override
    public void handle(HealthCheckResult result) {
        MonitoredService service = result.getService();
        ServiceStatus status = result.getEvaluatedStatus();

        if (isUnderMaintenance(service)) {
            log.debug("Suppressing alert evaluation for '{}': maintenance window active", service.getName());
            passToNext(result);
            return;
        }

        switch (status) {
            case DOWN -> processUnhealthyResult(service, Severity.CRITICAL,
                    service.getName() + " is DOWN");
            case DEGRADED -> processUnhealthyResult(service, Severity.WARNING,
                    service.getName() + " is DEGRADED (" + result.getResponseTimeMs() + "ms)");
            case HEALTHY, RECOVERED -> processHealthyResult(service);
            default -> { /* UNKNOWN - no alert */ }
        }

        passToNext(result);
    }

    private boolean isUnderMaintenance(MonitoredService service) {
        Instant now = Instant.now();
        return maintenanceWindowRepository
                .existsByService_IdAndStartsAtLessThanEqualAndEndsAtGreaterThan(service.getId(), now, now);
    }

    private void processUnhealthyResult(
            MonitoredService service,
            Severity severity,
            String message) {
        int failureCount = service.recordUnhealthyCheck();
        monitoredServiceRepository.save(service);

        List<Alert> alerts = alertsFor(service);
        Alert active = alerts.stream()
                .filter(alert -> alert.getStatus() != AlertStatus.RESOLVED)
                .findFirst()
                .orElse(null);

        if (failureCount < service.getAlertFailureThreshold()) {
            log.info("Alert pending for '{}': unhealthy check {}/{}",
                    service.getName(), failureCount, service.getAlertFailureThreshold());
            return;
        }

        if (active != null) {
            if (active.getSeverity() != severity) {
                resolveOpenAlerts(alerts, service);
                raiseNewAlert(service, severity, message);
            }
            return;
        }

        // At the exact threshold this is a new incident. Beyond it, a resolved
        // matching alert belongs to the same uninterrupted unhealthy sequence and
        // can be reopened rather than duplicated.
        if (failureCount > service.getAlertFailureThreshold()) {
            Alert latest = alerts.isEmpty() ? null : alerts.get(0);
            if (latest != null
                    && latest.getStatus() == AlertStatus.RESOLVED
                    && latest.getSeverity() == severity) {
                reopenAlert(latest, severity, message);
                return;
            }
        }

        raiseNewAlert(service, severity, message);
    }

    private void processHealthyResult(MonitoredService service) {
        if (service.getConsecutiveFailureCount() > 0) {
            service.resetConsecutiveFailureCount();
            monitoredServiceRepository.save(service);
        }
        resolveOpenAlerts(alertsFor(service), service);
    }

    private void raiseNewAlert(MonitoredService service, Severity severity, String message) {
        alertRepository.save(new Alert(service, severity, message));
        log.warn("{} alert raised: {}", severity, message);
    }

    private void reopenAlert(Alert alert, Severity severity, String message) {
        alert.setStatus(AlertStatus.OPEN);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setAcknowledgedAt(null);
        alert.setAcknowledgedBy(null);
        alert.setResolvedAt(null);
        alertRepository.save(alert);
        log.warn("{} alert reopened: {}", severity, message);
    }

    private List<Alert> alertsFor(MonitoredService service) {
        return alertRepository.findByService_IdOrderByCreatedAtDesc(service.getId());
    }

    // Mark all not-yet-resolved alerts for this service as RESOLVED.
    private void resolveOpenAlerts(List<Alert> alerts, MonitoredService service) {
        int resolved = 0;
        for (Alert alert : alerts) {
            if (alert.getStatus() != AlertStatus.RESOLVED) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
                resolved++;
            }
        }
        if (resolved > 0) {
            log.info("Resolved {} open alert(s) for service '{}'", resolved, service.getName());
        }
    }
}
