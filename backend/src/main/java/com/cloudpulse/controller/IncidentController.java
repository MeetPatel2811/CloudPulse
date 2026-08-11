package com.cloudpulse.controller;

import com.cloudpulse.command.AddIncidentNoteCommand;
import com.cloudpulse.command.AssignIncidentOwnerCommand;
import com.cloudpulse.model.IncidentActivity;
import com.cloudpulse.model.IncidentActivityType;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentActivityRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

// Incident operations layered on top of an alert: assign an owner, add notes, and
// read the activity timeline. Ownership and notes are stored as IncidentActivity
// records, so the feature stands on its own without changing the shared Alert entity.
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final AlertRepository alertRepository;
    private final IncidentActivityRepository activityRepository;

    public IncidentController(AlertRepository alertRepository,
                              IncidentActivityRepository activityRepository) {
        this.alertRepository = alertRepository;
        this.activityRepository = activityRepository;
    }

    // Current owner + full timeline for one incident.
    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        requireIncident(id);
        return buildResponse(id);
    }

    // Assign (or reassign) the incident owner.
    @PostMapping("/{id}/assign")
    public IncidentResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignOwnerRequest request) {
        new AssignIncidentOwnerCommand(id, request.owner(), alertRepository, activityRepository).execute();
        return buildResponse(id);
    }

    // Add a note to the incident's timeline.
    @PostMapping("/{id}/notes")
    public IncidentResponse addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
        String author = (request.author() == null || request.author().isBlank())
                ? "operator" : request.author();
        new AddIncidentNoteCommand(id, author, request.note(), alertRepository, activityRepository).execute();
        return buildResponse(id);
    }

    private IncidentResponse buildResponse(UUID id) {
        String owner = activityRepository
                .findFirstByAlert_IdAndTypeOrderByCreatedAtDesc(id, IncidentActivityType.ASSIGNED)
                .map(IncidentActivity::getAuthor)
                .orElse(null);
        List<IncidentActivityResponse> activity = activityRepository
                .findByAlert_IdOrderByCreatedAtAsc(id).stream()
                .map(IncidentActivityResponse::from)
                .toList();
        return new IncidentResponse(id, owner, activity);
    }

    // 404 (via GlobalExceptionHandler) if the incident/alert doesn't exist.
    private void requireIncident(UUID id) {
        if (alertRepository.findById(id).isEmpty()) {
            throw new NoSuchElementException("Incident not found: " + id);
        }
    }
}
