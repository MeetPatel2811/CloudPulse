package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

// Marks an alert as RESOLVED and records when. Used when someone clears an alert
// by hand (alerts are also auto-resolved on recovery by AlertCreationHandler).
public class ResolveAlertCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(ResolveAlertCommand.class);

    private final UUID alertId;
    private final AlertRepository alertRepository;

    public ResolveAlertCommand(UUID alertId, AlertRepository alertRepository) {
        this.alertId = alertId;
        this.alertRepository = alertRepository;
    }

    @Override
    public void execute() {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        alertRepository.save(alert);

        log.info("Alert {} resolved", alertId);
    }
}
