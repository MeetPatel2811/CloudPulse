package com.cloudpulse.controller;

import com.cloudpulse.model.EvaluationPolicy;
import jakarta.validation.constraints.NotNull;

/** Request body for changing the Strategy used by a monitored service. */
public record UpdateEvaluationPolicyRequest(
        @NotNull EvaluationPolicy evaluationPolicy) {
}
