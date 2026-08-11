package com.cloudpulse.evaluation;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class StrictHealthStrategy implements HealthEvaluationStrategy {

    private static final long DEGRADED_AFTER_MS = 500;

    @Override
    public EvaluationPolicy policy() {
        return EvaluationPolicy.STRICT;
    }

    @Override
    public long defaultLatencyThresholdMs() {
        return DEGRADED_AFTER_MS;
    }

    @Override
    public ServiceStatus evaluate(HealthCheckSnapshot snapshot, long latencyThresholdMs) {
        if (!snapshot.reachable() || snapshot.httpStatus() >= 400) {
            return ServiceStatus.DOWN;
        }
        if (snapshot.responseTimeMs() > latencyThresholdMs) {
            return ServiceStatus.DEGRADED;
        }
        return ServiceStatus.HEALTHY;
    }
}
