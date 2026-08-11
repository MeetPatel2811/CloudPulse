package com.cloudpulse.controller;

import com.cloudpulse.command.AcknowledgeAlertCommand;
import com.cloudpulse.command.ResolveAlertCommand;
import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Lists alerts, optionally filtered to a single {@link AlertStatus}. */
    @GetMapping
    public List<AlertResponse> listAlerts(@RequestParam(required = false) AlertStatus status) {
        List<Alert> alerts = status != null
                ? alertRepository.findByStatusOrderByCreatedAtDesc(status)
                : alertRepository.findAll();
        return alerts.stream().map(AlertResponse::from).collect(Collectors.toList());
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable UUID id,
                                     @RequestParam(defaultValue = "operator") String acknowledgedBy) {
        new AcknowledgeAlertCommand(id, acknowledgedBy, alertRepository).execute();
        return AlertResponse.from(findAlertOrThrow(id));
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable UUID id) {
        new ResolveAlertCommand(id, alertRepository).execute();
        return AlertResponse.from(findAlertOrThrow(id));
    }

    // Thrown when the id doesn't exist; GlobalExceptionHandler turns it into a 404 JSON response
    // (no local handler here, so alerts use the same error shape as the rest of the API).
    private Alert findAlertOrThrow(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + id));
    }
}
