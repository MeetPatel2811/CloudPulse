package com.cloudpulse.handler;

import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.evaluation.NormalHealthStrategy;
import com.cloudpulse.evaluation.StrictHealthStrategy;
import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.ServiceStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatencyHandlerTest {

    private final HealthEvaluationService evaluationService =
            new HealthEvaluationService(List.of(
                    new NormalHealthStrategy(),
                    new StrictHealthStrategy()
            ));

    private final LatencyHandler handler =
            new LatencyHandler(evaluationService);

    private HealthCheckResult resultFor(
            EvaluationPolicy policy,
            boolean reachable,
            int httpStatus,
            long responseTimeMs
    ) {
        MonitoredService service = new MonitoredService(
                "Payment",
                "http://localhost:8081/health",
                MonitorType.MOCK
        );
        service.setActiveEvaluationStrategy(policy);

        HealthCheckResult result = new HealthCheckResult(
                reachable,
                httpStatus,
                responseTimeMs,
                "test"
        );
        result.setService(service);
        return result;
    }

    @Test
    void fastResponseIsHealthy() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.NORMAL,
                true,
                200,
                100
        );

        handler.handle(result);

        assertEquals(ServiceStatus.HEALTHY, result.getEvaluatedStatus());
    }

    @Test
    void slowResponseIsDegraded() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.NORMAL,
                true,
                200,
                2_000
        );

        handler.handle(result);

        assertEquals(ServiceStatus.DEGRADED, result.getEvaluatedStatus());
    }

    @Test
    void alreadyDownIsNotOverwritten() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.NORMAL,
                false,
                0,
                0
        );
        result.setEvaluatedStatus(ServiceStatus.DOWN);

        handler.handle(result);

        assertEquals(ServiceStatus.DOWN, result.getEvaluatedStatus());
    }

    @Test
    void normalPolicyAcceptsModerateLatency() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.NORMAL,
                true,
                200,
                750
        );

        handler.handle(result);

        assertEquals(ServiceStatus.HEALTHY, result.getEvaluatedStatus());
    }

    @Test
    void strictPolicyDegradesModerateLatency() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.STRICT,
                true,
                200,
                750
        );

        handler.handle(result);

        assertEquals(ServiceStatus.DEGRADED, result.getEvaluatedStatus());
    }

    @Test
    void serviceThresholdOverridesTheStrategyDefault() {
        HealthCheckResult result = resultFor(
                EvaluationPolicy.NORMAL,
                true,
                200,
                750
        );
        result.getService().setLatencyThresholdMs(500L);

        handler.handle(result);

        assertEquals(ServiceStatus.DEGRADED, result.getEvaluatedStatus());
    }
}
