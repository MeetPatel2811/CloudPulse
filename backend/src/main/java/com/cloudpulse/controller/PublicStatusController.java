package com.cloudpulse.controller;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/public/status")
public class PublicStatusController {

    private final MonitoredServiceRepository serviceRepository;

    public PublicStatusController(MonitoredServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public PublicStatusResponse getStatus() {
        List<MonitoredService> enabledServices = serviceRepository.findByEnabledTrue();
        List<PublicServiceStatusResponse> services = enabledServices.stream()
                .sorted(Comparator.comparing(MonitoredService::getName, String.CASE_INSENSITIVE_ORDER))
                .map(service -> new PublicServiceStatusResponse(
                        service.getName(),
                        service.getServiceGroup(),
                        service.getCurrentStatus(),
                        service.getLastCheckedAt()))
                .toList();

        return new PublicStatusResponse(Instant.now(), overallStatus(enabledServices), services);
    }

    private String overallStatus(List<MonitoredService> services) {
        if (services.isEmpty()) {
            return "NO_SERVICES";
        }
        if (services.stream().anyMatch(service -> service.getCurrentStatus() == ServiceStatus.DOWN)) {
            return "MAJOR_OUTAGE";
        }
        if (services.stream().anyMatch(service -> service.getCurrentStatus() == ServiceStatus.DEGRADED)) {
            return "DEGRADED_PERFORMANCE";
        }
        if (services.stream().anyMatch(service -> service.getCurrentStatus() == ServiceStatus.UNKNOWN)) {
            return "CHECKING";
        }
        return "OPERATIONAL";
    }
}

