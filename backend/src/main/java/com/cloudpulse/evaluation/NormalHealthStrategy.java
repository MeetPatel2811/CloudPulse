package com.cloudpulse.evaluation;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;
import org.springframework.stereotype.Component;

@Component
public class NormalHealthStrategy implements HealthEvaluationStrategy {

    private static final long DEGRADED_AFTER_MS = 1_000;

    @Override
    public EvaluationPolicy policy() {
        return EvaluationPolicy.NORMAL;
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
