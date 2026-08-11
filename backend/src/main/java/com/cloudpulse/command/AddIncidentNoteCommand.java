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

// Adds a free-text note to an incident's activity timeline.
public class AddIncidentNoteCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(AddIncidentNoteCommand.class);

    private final UUID alertId;
    private final String author;
    private final String note;
    private final AlertRepository alertRepository;
    private final IncidentActivityRepository activityRepository;

    public AddIncidentNoteCommand(UUID alertId, String author, String note,
                                  AlertRepository alertRepository,
                                  IncidentActivityRepository activityRepository) {
        this.alertId = alertId;
        this.author = author;
        this.note = note;
        this.alertRepository = alertRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public void execute() {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Note must not be blank");
        }
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));

        activityRepository.save(
                new IncidentActivity(alert, IncidentActivityType.NOTE, author, note));

        log.info("Note added to incident {} by {}", alertId, author);
    }
}
