package com.cloudpulse.controller;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;

import java.time.Instant;
import java.util.UUID;

/** Operational summary used by service cards and response-time charts. */
public record ServiceMetricsResponse(
        UUID serviceId,
        String serviceName,
        String window,
        Instant windowStart,
        Instant windowEnd,
        ServiceStatus currentStatus,
        boolean enabled,
        EvaluationPolicy activeEvaluationStrategy,
        long totalChecks,
        long successfulChecks,
        long failedChecks,
        double availabilityPercent,
        double availabilitySloPercent,
        double errorBudgetRemainingPercent,
        boolean sloMet,
        Double averageResponseTimeMs,
        Long p95ResponseTimeMs,
        int p95SampleSize,
        boolean p95Sampled) {
}
