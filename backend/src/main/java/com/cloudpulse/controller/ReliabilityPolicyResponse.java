package com.cloudpulse.controller;

import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.MonitoredService;

import java.util.UUID;

/** Exposes both the stored override and the threshold that health checks actually use. */
public record ReliabilityPolicyResponse(
        UUID serviceId,
        String serviceName,
        EvaluationPolicy evaluationPolicy,
        Long latencyThresholdMs,
        long effectiveLatencyThresholdMs,
        int alertFailureThreshold,
        int consecutiveFailureCount) {

    public static ReliabilityPolicyResponse from(
            MonitoredService service,
            HealthEvaluationService evaluationService) {
        return new ReliabilityPolicyResponse(
                service.getId(),
                service.getName(),
                service.getActiveEvaluationStrategy(),
                service.getLatencyThresholdMs(),
                evaluationService.effectiveLatencyThresholdMs(
                        service.getActiveEvaluationStrategy(),
                        service.getLatencyThresholdMs()),
                service.getAlertFailureThreshold(),
                service.getConsecutiveFailureCount());
    }
}
