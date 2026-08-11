package com.cloudpulse.evaluation;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;

public interface HealthEvaluationStrategy {

    EvaluationPolicy policy();

    long defaultLatencyThresholdMs();

    ServiceStatus evaluate(HealthCheckSnapshot snapshot, long latencyThresholdMs);
}
