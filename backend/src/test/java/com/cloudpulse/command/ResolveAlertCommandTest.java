package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.Severity;
import com.cloudpulse.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ResolveAlertCommandTest {

    @Test
    void execute_setsResolvedFields() {
        MonitoredService service =
                new MonitoredService("Notification", "http://localhost:8083/health", MonitorType.MOCK);
        Alert alert = new Alert(service, Severity.WARNING, "Notification service is DEGRADED");
        UUID id = UUID.randomUUID();
        AlertRepository alertRepository = mock(AlertRepository.class);
        when(alertRepository.findById(id)).thenReturn(Optional.of(alert));

        new ResolveAlertCommand(id, alertRepository).execute();

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertNotNull(alert.getResolvedAt());
        verify(alertRepository).save(alert);
    }
}
