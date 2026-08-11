package com.cloudpulse.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudpulse.model.ServiceStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceStateMachineTest {

    private ServiceStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ServiceStateMachine(List.of(
                new UnknownState(),
                new HealthyState(),
                new DegradedState(),
                new DownState(),
                new RecoveredState()
        ));
    }

    @Test
    void healthyObservationAfterDownProducesRecoveredState() {
        assertThat(stateMachine.transition(ServiceStatus.DOWN, ServiceStatus.HEALTHY))
                .isEqualTo(ServiceStatus.RECOVERED);
    }

    @Test
    void aSecondHealthyObservationSettlesRecoveredToHealthy() {
        assertThat(stateMachine.transition(ServiceStatus.RECOVERED, ServiceStatus.HEALTHY))
                .isEqualTo(ServiceStatus.HEALTHY);
    }

    @Test
    void degradedObservationFromHealthyRemainsDegraded() {
        assertThat(stateMachine.transition(ServiceStatus.HEALTHY, ServiceStatus.DEGRADED))
                .isEqualTo(ServiceStatus.DEGRADED);
    }
}
