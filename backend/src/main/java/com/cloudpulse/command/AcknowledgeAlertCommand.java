package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

// Marks an alert as ACKNOWLEDGED and records who did it and when.
public class AcknowledgeAlertCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(AcknowledgeAlertCommand.class);

    private final UUID alertId;
    private final String acknowledgedBy;
    private final AlertRepository alertRepository;

    public AcknowledgeAlertCommand(UUID alertId, String acknowledgedBy, AlertRepository alertRepository) {
        this.alertId = alertId;
        this.acknowledgedBy = acknowledgedBy;
        this.alertRepository = alertRepository;
    }

    @Override
    public void execute() {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(acknowledgedBy);
        alertRepository.save(alert);

        log.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
    }
}
