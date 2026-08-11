package com.cloudpulse.controller;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;

import java.util.UUID;

/** API response confirming which evaluation Strategy a service will use. */
public record EvaluationPolicyResponse(
        UUID serviceId,
        String serviceName,
        EvaluationPolicy evaluationPolicy,
        ServiceStatus currentStatus) {

    public static EvaluationPolicyResponse from(MonitoredService service) {
        return new EvaluationPolicyResponse(
                service.getId(),
                service.getName(),
                service.getActiveEvaluationStrategy(),
                service.getCurrentStatus());
    }
}
