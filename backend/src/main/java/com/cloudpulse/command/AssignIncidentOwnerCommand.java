package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.IncidentActivity;
import com.cloudpulse.model.IncidentActivityType;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

// Assigns an owner to an incident by recording an ASSIGNED entry on its timeline.
// The current owner is the author of the most recent ASSIGNED entry, so ownership
// is tracked in the incident's own activity records without changing the Alert entity.
public class AssignIncidentOwnerCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(AssignIncidentOwnerCommand.class);

    private final UUID alertId;
    private final String owner;
    private final AlertRepository alertRepository;
    private final IncidentActivityRepository activityRepository;

    public AssignIncidentOwnerCommand(UUID alertId, String owner,
                                      AlertRepository alertRepository,
                                      IncidentActivityRepository activityRepository) {
        this.alertId = alertId;
        this.owner = owner;
        this.alertRepository = alertRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public void execute() {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));

        activityRepository.save(
                new IncidentActivity(alert, IncidentActivityType.ASSIGNED, owner, "Assigned to " + owner));

        log.info("Incident {} assigned to {}", alertId, owner);
    }
}
