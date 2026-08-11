package com.cloudpulse.controller;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/services/{serviceId}/slo")
public class SloTargetController {

    private final MonitoredServiceRepository serviceRepository;

    public SloTargetController(MonitoredServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @PutMapping
    public MonitoredService updateTarget(
            @PathVariable UUID serviceId,
            @Valid @RequestBody SloTargetRequest request) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
        service.setAvailabilitySloPercent(request.availabilitySloPercent());
        return serviceRepository.save(service);
    }
}
