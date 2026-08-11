package com.cloudpulse.handler;

import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.evaluation.NormalHealthStrategy;
import com.cloudpulse.evaluation.StrictHealthStrategy;
import com.cloudpulse.lifecycle.DegradedState;
import com.cloudpulse.lifecycle.DownState;
import com.cloudpulse.lifecycle.HealthyState;
import com.cloudpulse.lifecycle.RecoveredState;
import com.cloudpulse.lifecycle.ServiceStateMachine;
import com.cloudpulse.lifecycle.UnknownState;
import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.Severity;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.observer.StatusEventPublisher;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.MaintenanceWindowRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests for the whole Chain of Responsibility, run through HealthCheckProcessor
// so all five handlers work together.
class HealthCheckProcessorTest {

    private AlertRepository alertRepository;
    private StatusEventRepository statusEventRepository;
    private MonitoredServiceRepository monitoredServiceRepository;
    private MaintenanceWindowRepository maintenanceWindowRepository;
    private HealthCheckProcessor processor;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        statusEventRepository = mock(StatusEventRepository.class);
        monitoredServiceRepository = mock(MonitoredServiceRepository.class);
        maintenanceWindowRepository = mock(MaintenanceWindowRepository.class);
        processor = new HealthCheckProcessor(
                statusEventRepository,
                monitoredServiceRepository,
                alertRepository,
                maintenanceWindowRepository,
                new HealthEvaluationService(List.of(
                        new NormalHealthStrategy(),
                        new StrictHealthStrategy()
                )),
                new ServiceStateMachine(List.of(
                        new UnknownState(),
                        new HealthyState(),
                        new DegradedState(),
                        new DownState(),
                        new RecoveredState()
                )),
                new StatusEventPublisher()
        );
    }

    private MonitoredService serviceWithStatus(ServiceStatus status) {
        MonitoredService service =
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
        service.setCurrentStatus(status);
        return service;
    }

    private HealthCheckResult resultFor(MonitoredService service, boolean reachable, int http, long ms) {
        HealthCheckResult result = new HealthCheckResult(reachable, http, ms, "test");
        result.setService(service);
        return result;
    }

    @Test
    void fastReachableService_marksHealthyAndRecordsTransition() {
        MonitoredService service = serviceWithStatus(ServiceStatus.UNKNOWN);

        processor.process(resultFor(service, true, 200, 120));

        assertEquals(ServiceStatus.HEALTHY, service.getCurrentStatus());
        verify(statusEventRepository).save(any(StatusEvent.class));
        // Becoming healthy resolves open alerts; there are none, so nothing is saved.
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void unreachableService_marksDownAndRaisesCriticalAlert() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);

        processor.process(resultFor(service, false, 0, 5000));

        assertEquals(ServiceStatus.DOWN, service.getCurrentStatus());
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(Severity.CRITICAL, captor.getValue().getSeverity());
    }

    @Test
    void slowService_marksDegradedAndRaisesWarningAlert() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);

        processor.process(resultFor(service, true, 200, 1500));

        assertEquals(ServiceStatus.DEGRADED, service.getCurrentStatus());
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(Severity.WARNING, captor.getValue().getSeverity());
    }

    @Test
    void statusUnchanged_recordsNothing() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);

        processor.process(resultFor(service, true, 200, 100));

        verify(statusEventRepository, never()).save(any());
        verify(monitoredServiceRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void unreachableService_underMaintenance_suppressesAlertButStillTransitions() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);
        when(maintenanceWindowRepository.existsByService_IdAndStartsAtLessThanEqualAndEndsAtGreaterThan(
                any(), any(), any())).thenReturn(true);

        processor.process(resultFor(service, false, 0, 5000));

        // The lifecycle/status-event side still runs; only alerting is suppressed.
        assertEquals(ServiceStatus.DOWN, service.getCurrentStatus());
        verify(statusEventRepository).save(any(StatusEvent.class));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void recoveryFromDown_resolvesOpenAlerts() {
        MonitoredService service = serviceWithStatus(ServiceStatus.DOWN);
        Alert open = new Alert(service, Severity.CRITICAL, "was down");
        when(alertRepository.findByService_IdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(open));

        processor.process(resultFor(service, true, 200, 100));

        assertEquals(ServiceStatus.RECOVERED, service.getCurrentStatus());
        assertEquals(AlertStatus.RESOLVED, open.getStatus());
        assertNotNull(open.getResolvedAt());
        verify(alertRepository).save(open);
    }

    @Test
    void delaysAlertUntilTheConfiguredUnhealthyCheckThreshold() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);
        service.setAlertFailureThreshold(2);

        processor.process(resultFor(service, false, 0, 5_000));

        assertEquals(ServiceStatus.DOWN, service.getCurrentStatus());
        assertEquals(1, service.getConsecutiveFailureCount());
        verify(alertRepository, never()).save(any(Alert.class));

        processor.process(resultFor(service, false, 0, 5_000));

        assertEquals(2, service.getConsecutiveFailureCount());
        verify(alertRepository).save(argThat(alert -> alert.getSeverity() == Severity.CRITICAL));
    }

    @Test
    void reopensResolvedAlertWhenTheSameUnhealthySequenceContinues() {
        MonitoredService service = serviceWithStatus(ServiceStatus.HEALTHY);
        service.setAlertFailureThreshold(1);
        when(alertRepository.findByService_IdOrderByCreatedAtDesc(nullable(UUID.class)))
                .thenReturn(List.of());

        processor.process(resultFor(service, false, 0, 5_000));

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert incident = captor.getValue();
        incident.setStatus(AlertStatus.RESOLVED);
        incident.setResolvedAt(java.time.Instant.now());
        clearInvocations(alertRepository);
        when(alertRepository.findByService_IdOrderByCreatedAtDesc(nullable(UUID.class)))
                .thenReturn(List.of(incident));

        processor.process(resultFor(service, false, 0, 5_000));

        assertEquals(AlertStatus.OPEN, incident.getStatus());
        assertNull(incident.getResolvedAt());
        verify(alertRepository).save(incident);
    }

    @Test
    void severityEscalationDoesNotReopenAnOlderHistoricalAlert() {
        MonitoredService service = serviceWithStatus(ServiceStatus.DEGRADED);
        service.setAlertFailureThreshold(1);
        service.recordUnhealthyCheck();
        Alert latestWarning = new Alert(service, Severity.WARNING, "was degraded");
        latestWarning.setStatus(AlertStatus.RESOLVED);
        Alert olderCritical = new Alert(service, Severity.CRITICAL, "older outage");
        olderCritical.setStatus(AlertStatus.RESOLVED);
        when(alertRepository.findByService_IdOrderByCreatedAtDesc(nullable(UUID.class)))
                .thenReturn(List.of(latestWarning, olderCritical));

        processor.process(resultFor(service, false, 0, 5_000));

        assertEquals(AlertStatus.RESOLVED, olderCritical.getStatus());
        verify(alertRepository, never()).save(olderCritical);
        verify(alertRepository).save(argThat(alert ->
                alert != latestWarning
                        && alert != olderCritical
                        && alert.getSeverity() == Severity.CRITICAL));
    }
}
