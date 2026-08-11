package com.cloudpulse.handler;

import com.cloudpulse.evaluation.HealthCheckSnapshot;
import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.ServiceStatus;

// Evaluates reachable results using the service's selected Strategy.
// AvailabilityHandler handles unreachable services before this stage.
public class LatencyHandler extends ResultHandler {

    private final HealthEvaluationService evaluationService;

    public LatencyHandler(HealthEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Override
    public void handle(HealthCheckResult result) {
        if (!result.isReachable()
                || result.getEvaluatedStatus() == ServiceStatus.DOWN) {
            passToNext(result);
            return;
        }

        HealthCheckSnapshot snapshot = new HealthCheckSnapshot(
                result.isReachable(),
                result.getHttpStatus(),
                result.getResponseTimeMs()
        );

        ServiceStatus status = evaluationService.evaluate(
                result.getService().getActiveEvaluationStrategy(),
                snapshot,
                result.getService().getLatencyThresholdMs()
        );

        result.setEvaluatedStatus(status);
        passToNext(result);
    }
}
