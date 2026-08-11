package com.cloudpulse.controller;

import com.cloudpulse.model.MaintenanceWindow;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MaintenanceWindowRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Schedules maintenance windows for a service. While a window is active,
 * {@code AlertCreationHandler} suppresses alerting for that service.
 */
@RestController
@RequestMapping("/api/services/{serviceId}/maintenance-windows")
public class MaintenanceWindowController {

    private final MaintenanceWindowRepository windowRepository;
    private final MonitoredServiceRepository serviceRepository;

    public MaintenanceWindowController(
            MaintenanceWindowRepository windowRepository,
            MonitoredServiceRepository serviceRepository) {
        this.windowRepository = windowRepository;
        this.serviceRepository = serviceRepository;
    }

    /** All windows for the service (scheduled, active, and ended), newest start first. */
    @GetMapping
    public List<MaintenanceWindowResponse> listForService(@PathVariable UUID serviceId) {
        requireService(serviceId);
        return windowRepository.findByService_IdOrderByStartsAtDesc(serviceId).stream()
                .map(MaintenanceWindowResponse::from)
                .toList();
    }

    @PostMapping
    public MaintenanceWindowResponse schedule(
            @PathVariable UUID serviceId,
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        MonitoredService service = requireService(serviceId);

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        boolean overlaps = !windowRepository
                .findByService_IdAndStartsAtLessThanAndEndsAtGreaterThan(
                        serviceId, request.endsAt(), request.startsAt())
                .isEmpty();
        if (overlaps) {
            throw new MaintenanceWindowConflictException(
                    "This service already has a maintenance window overlapping that time range");
        }

        MaintenanceWindow saved = windowRepository.save(
                new MaintenanceWindow(service, request.startsAt(), request.endsAt(), request.reason()));
        return MaintenanceWindowResponse.from(saved);
    }

    /** Cancels a scheduled or active window early. */
    @DeleteMapping("/{windowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID serviceId, @PathVariable UUID windowId) {
        requireService(serviceId);
        MaintenanceWindow window = windowRepository.findById(windowId)
                .filter(candidate -> candidate.getService().getId().equals(serviceId))
                .orElseThrow(() -> new NoSuchElementException("Maintenance window not found: " + windowId));
        windowRepository.delete(window);
    }

    private MonitoredService requireService(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
    }
}
