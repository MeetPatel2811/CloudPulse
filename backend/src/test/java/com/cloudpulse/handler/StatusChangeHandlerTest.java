package com.cloudpulse.handler;

import com.cloudpulse.lifecycle.DegradedState;
import com.cloudpulse.lifecycle.DownState;
import com.cloudpulse.lifecycle.HealthyState;
import com.cloudpulse.lifecycle.RecoveredState;
import com.cloudpulse.lifecycle.ServiceStateMachine;
import com.cloudpulse.lifecycle.UnknownState;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.observer.StatusEventPublisher;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Focused tests for StatusChangeHandler's new job: notifying the Observer. With no
// active transaction (a plain unit test) the publish happens immediately.
class StatusChangeHandlerTest {

    private StatusEventRepository statusEventRepository;
    private MonitoredServiceRepository monitoredServiceRepository;
    private StatusEventPublisher publisher;
    private StatusChangeHandler handler;

    @BeforeEach
    void setUp() {
        statusEventRepository = mock(StatusEventRepository.class);
        monitoredServiceRepository = mock(MonitoredServiceRepository.class);
        publisher = mock(StatusEventPublisher.class);
        ServiceStateMachine stateMachine = new ServiceStateMachine(List.of(
                new UnknownState(), new HealthyState(), new DegradedState(),
                new DownState(), new RecoveredState()));
        handler = new StatusChangeHandler(
                statusEventRepository, monitoredServiceRepository, stateMachine, publisher);
        when(statusEventRepository.save(any(StatusEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private HealthCheckResult resultFor(ServiceStatus current, ServiceStatus observed) {
        MonitoredService service =
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
        service.setCurrentStatus(current);
        HealthCheckResult result = new HealthCheckResult(true, 200, 100, "test");
        result.setService(service);
        result.setEvaluatedStatus(observed);
        return result;
    }

    @Test
    void publishesEvent_whenStatusChanges() {
        handler.handle(resultFor(ServiceStatus.HEALTHY, ServiceStatus.DEGRADED));
        verify(publisher, times(1)).publish(any(StatusEvent.class));
    }

    @Test
    void doesNotPublish_whenStatusUnchanged() {
        handler.handle(resultFor(ServiceStatus.HEALTHY, ServiceStatus.HEALTHY));
        verify(publisher, never()).publish(any());
    }
}
