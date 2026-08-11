package com.cloudpulse.controller;

import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Reads and changes the per-service reliability settings used during health evaluation. */
@RestController
@RequestMapping("/api/services/{serviceId}/reliability-policy")
public class ReliabilityPolicyController {

    private final MonitoredServiceRepository serviceRepository;
    private final HealthEvaluationService evaluationService;

    public ReliabilityPolicyController(
            MonitoredServiceRepository serviceRepository,
            HealthEvaluationService evaluationService) {
        this.serviceRepository = serviceRepository;
        this.evaluationService = evaluationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ReliabilityPolicyResponse getPolicy(@PathVariable UUID serviceId) {
        return ReliabilityPolicyResponse.from(findService(serviceId), evaluationService);
    }

    @PatchMapping
    @Transactional
    public ReliabilityPolicyResponse updatePolicy(
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateReliabilityPolicyRequest request) {
        MonitoredService service = findService(serviceId);
        service.setLatencyThresholdMs(request.latencyThresholdMs());
        if (request.alertFailureThreshold() != null
                && request.alertFailureThreshold() != service.getAlertFailureThreshold()) {
            service.setAlertFailureThreshold(request.alertFailureThreshold());
            // A changed rule begins a fresh sequence; carrying progress from the
            // previous threshold would make the new policy alert unpredictably.
            service.resetConsecutiveFailureCount();
        }
        return ReliabilityPolicyResponse.from(serviceRepository.save(service), evaluationService);
    }

    private MonitoredService findService(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
    }
}
