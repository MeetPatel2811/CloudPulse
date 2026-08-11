package com.cloudpulse.controller;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Changes the health-evaluation Strategy used by an existing service. */
@RestController
@RequestMapping("/api/services/{serviceId}/evaluation-policy")
public class EvaluationPolicyController {

    private final MonitoredServiceRepository serviceRepository;

    public EvaluationPolicyController(MonitoredServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Persists the selected Strategy. The service's current status is not recalculated
     * immediately; the new policy is applied by LatencyHandler on the next health check.
     */
    @PatchMapping
    @Transactional
    public EvaluationPolicyResponse updatePolicy(
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateEvaluationPolicyRequest request) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));

        service.setActiveEvaluationStrategy(request.evaluationPolicy());
        MonitoredService saved = serviceRepository.save(service);
        return EvaluationPolicyResponse.from(saved);
    }
}
