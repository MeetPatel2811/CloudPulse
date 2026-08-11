package com.cloudpulse.evaluation;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HealthEvaluationService {

    private final Map<EvaluationPolicy, HealthEvaluationStrategy> strategies;

    public HealthEvaluationService(List<HealthEvaluationStrategy> strategies) {
        this.strategies = new EnumMap<>(EvaluationPolicy.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.policy(), strategy));
    }

    public ServiceStatus evaluate(
            EvaluationPolicy policy,
            HealthCheckSnapshot snapshot
    ) {
        return evaluate(policy, snapshot, null);
    }

    public ServiceStatus evaluate(
            EvaluationPolicy policy,
            HealthCheckSnapshot snapshot,
            Long latencyThresholdOverrideMs
    ) {
        HealthEvaluationStrategy strategy = strategies.get(policy);
        if (strategy == null) {
            throw new IllegalArgumentException("No evaluation strategy registered for " + policy);
        }
        return strategy.evaluate(
                snapshot,
                effectiveLatencyThresholdMs(policy, latencyThresholdOverrideMs)
        );
    }

    public long effectiveLatencyThresholdMs(
            EvaluationPolicy policy,
            Long latencyThresholdOverrideMs
    ) {
        HealthEvaluationStrategy strategy = strategies.get(policy);
        if (strategy == null) {
            throw new IllegalArgumentException("No evaluation strategy registered for " + policy);
        }
        return latencyThresholdOverrideMs != null
                ? latencyThresholdOverrideMs
                : strategy.defaultLatencyThresholdMs();
    }

}
