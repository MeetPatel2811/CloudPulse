package com.cloudpulse.controller;

import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

// Read-only history endpoint: the recorded status transitions for one service,
// newest first. Backs the dashboard's event timeline. Errors are turned into a
// consistent JSON body by GlobalExceptionHandler.
@RestController
public class StatusEventController {

    private final StatusEventRepository statusEventRepository;
    private final MonitoredServiceRepository serviceRepository;

    public StatusEventController(StatusEventRepository statusEventRepository,
                                 MonitoredServiceRepository serviceRepository) {
        this.statusEventRepository = statusEventRepository;
        this.serviceRepository = serviceRepository;
    }

    // readOnly transaction so the lazy service association loads during DTO mapping
    // without depending on the open-in-view setting.
    @GetMapping("/api/services/{serviceId}/status-events")
    @Transactional(readOnly = true)
    public List<StatusEventResponse> listForService(@PathVariable UUID serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new NoSuchElementException("Service not found: " + serviceId);
        }
        List<StatusEvent> events =
                statusEventRepository.findByService_IdOrderByOccurredAtDesc(serviceId);
        return events.stream().map(StatusEventResponse::from).collect(Collectors.toList());
    }
}
