package com.cloudpulse.command;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.IncidentActivity;
import com.cloudpulse.model.IncidentActivityType;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.Severity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentActivityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssignIncidentOwnerCommandTest {

    private Alert sampleAlert() {
        MonitoredService service =
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
        return new Alert(service, Severity.CRITICAL, "Payment is DOWN");
    }

    @Test
    void execute_recordsAssignedActivity() {
        Alert alert = sampleAlert();
        UUID id = UUID.randomUUID();
        AlertRepository alertRepository = mock(AlertRepository.class);
        IncidentActivityRepository activityRepository = mock(IncidentActivityRepository.class);
        when(alertRepository.findById(id)).thenReturn(Optional.of(alert));

        new AssignIncidentOwnerCommand(id, "alice", alertRepository, activityRepository).execute();

        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(activityRepository).save(captor.capture());
        assertEquals(IncidentActivityType.ASSIGNED, captor.getValue().getType());
        assertEquals("alice", captor.getValue().getAuthor());
    }

    @Test
    void execute_rejectsBlankOwner() {
        AlertRepository alertRepository = mock(AlertRepository.class);
        IncidentActivityRepository activityRepository = mock(IncidentActivityRepository.class);

        AssignIncidentOwnerCommand command =
                new AssignIncidentOwnerCommand(UUID.randomUUID(), "   ", alertRepository, activityRepository);

        assertThrows(IllegalArgumentException.class, command::execute);
        verifyNoInteractions(activityRepository);
    }

    @Test
    void execute_throwsWhenAlertMissing() {
        UUID id = UUID.randomUUID();
        AlertRepository alertRepository = mock(AlertRepository.class);
        IncidentActivityRepository activityRepository = mock(IncidentActivityRepository.class);
        when(alertRepository.findById(id)).thenReturn(Optional.empty());

        AssignIncidentOwnerCommand command =
                new AssignIncidentOwnerCommand(id, "alice", alertRepository, activityRepository);

        assertThrows(NoSuchElementException.class, command::execute);
    }
}
