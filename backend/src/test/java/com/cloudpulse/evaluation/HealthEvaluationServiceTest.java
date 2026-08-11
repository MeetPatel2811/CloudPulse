package com.cloudpulse.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.ServiceStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HealthEvaluationServiceTest {

    private HealthEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new HealthEvaluationService(List.of(
                new NormalHealthStrategy(),
                new StrictHealthStrategy()
        ));
    }

    @Test
    void normalPolicyAcceptsModerateLatency() {
        ServiceStatus status = service.evaluate(
                EvaluationPolicy.NORMAL,
                new HealthCheckSnapshot(true, 200, 750)
        );

        assertThat(status).isEqualTo(ServiceStatus.HEALTHY);
    }

    @Test
    void strictPolicyDegradesTheSameLatency() {
        ServiceStatus status = service.evaluate(
                EvaluationPolicy.STRICT,
                new HealthCheckSnapshot(true, 200, 750)
        );

        assertThat(status).isEqualTo(ServiceStatus.DEGRADED);
    }

    @Test
    void customThresholdOverridesTheSelectedStrategyDefault() {
        HealthCheckSnapshot snapshot = new HealthCheckSnapshot(true, 200, 750);

        assertThat(service.evaluate(EvaluationPolicy.NORMAL, snapshot, 500L))
                .isEqualTo(ServiceStatus.DEGRADED);
        assertThat(service.evaluate(EvaluationPolicy.STRICT, snapshot, 1_000L))
                .isEqualTo(ServiceStatus.HEALTHY);
    }

    @Test
    void reportsTheEffectiveDefaultOrCustomThreshold() {
        assertThat(service.effectiveLatencyThresholdMs(EvaluationPolicy.NORMAL, null))
                .isEqualTo(1_000L);
        assertThat(service.effectiveLatencyThresholdMs(EvaluationPolicy.STRICT, null))
                .isEqualTo(500L);
        assertThat(service.effectiveLatencyThresholdMs(EvaluationPolicy.STRICT, 900L))
                .isEqualTo(900L);
    }

    @Test
    void unreachableServiceIsDownForEveryPolicy() {
        HealthCheckSnapshot snapshot = new HealthCheckSnapshot(false, 0, 2_000);

        assertThat(service.evaluate(EvaluationPolicy.NORMAL, snapshot))
                .isEqualTo(ServiceStatus.DOWN);
        assertThat(service.evaluate(EvaluationPolicy.STRICT, snapshot))
                .isEqualTo(ServiceStatus.DOWN);
    }

    @Test
    void httpErrorIsDownForEveryPolicy() {
        HealthCheckSnapshot snapshot = new HealthCheckSnapshot(true, 404, 100);

        assertThat(service.evaluate(EvaluationPolicy.NORMAL, snapshot))
                .isEqualTo(ServiceStatus.DOWN);
        assertThat(service.evaluate(EvaluationPolicy.STRICT, snapshot))
                .isEqualTo(ServiceStatus.DOWN);
    }
}
