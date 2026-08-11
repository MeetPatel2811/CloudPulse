package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Read-only API for a service's persisted health-check history. */
@RestController
@RequestMapping("/api/services/{serviceId}/health-checks")
public class HealthCheckHistoryController {

    private static final int MAX_LIMIT = 1_000;

    private final HealthCheckResultRepository resultRepository;
    private final MonitoredServiceRepository serviceRepository;

    public HealthCheckHistoryController(
            HealthCheckResultRepository resultRepository,
            MonitoredServiceRepository serviceRepository) {
        this.resultRepository = resultRepository;
        this.serviceRepository = serviceRepository;
    }

    /** Returns a bounded, optionally time-filtered history newest first. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<HealthCheckResultResponse> listForService(
            @PathVariable UUID serviceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "200") int limit) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new NoSuchElementException("Service not found: " + serviceId);
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_LIMIT);
        }

        Instant fromInstant = parseInstant(from, Instant.EPOCH, "from");
        Instant toInstant = parseInstant(to, Instant.now(), "to");
        if (fromInstant.isAfter(toInstant)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        List<HealthCheckResult> results =
                resultRepository.findByService_IdAndCheckedAtBetweenOrderByCheckedAtDesc(
                        serviceId,
                        fromInstant,
                        toInstant,
                        PageRequest.of(0, limit));
        return results.stream().map(HealthCheckResultResponse::from).toList();
    }

    private Instant parseInstant(String value, Instant defaultValue, String parameterName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    parameterName + " must be an ISO-8601 instant, for example 2026-08-09T12:00:00Z",
                    exception);
        }
    }
}
