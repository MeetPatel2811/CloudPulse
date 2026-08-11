package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.Severity;
import com.cloudpulse.repository.AlertRepository;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcknowledgeAlertCommandTest {

    @Test
    void execute_setsAcknowledgedFields() {
        MonitoredService service =
                new MonitoredService("User", "http://localhost:8082/health", MonitorType.MOCK);
        Alert alert = new Alert(service, Severity.CRITICAL, "User service is DOWN");
        UUID id = UUID.randomUUID();
        AlertRepository alertRepository = mock(AlertRepository.class);
        when(alertRepository.findById(id)).thenReturn(Optional.of(alert));

        new AcknowledgeAlertCommand(id, "meet", alertRepository).execute();

        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
        assertEquals("meet", alert.getAcknowledgedBy());
        assertNotNull(alert.getAcknowledgedAt());
        verify(alertRepository).save(alert);
    }

    @Test
    void execute_throwsWhenAlertMissing() {
        UUID id = UUID.randomUUID();
        AlertRepository alertRepository = mock(AlertRepository.class);
        when(alertRepository.findById(id)).thenReturn(Optional.empty());

        AcknowledgeAlertCommand command = new AcknowledgeAlertCommand(id, "meet", alertRepository);

        assertThrows(NoSuchElementException.class, command::execute);
    }
}
