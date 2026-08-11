package com.cloudpulse.command;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MonitoringToggleCommandTest {

    private MonitoredService newService() {
        return new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
    }

    @Test
    void disable_turnsOffMonitoring() {
        MonitoredService service = newService();
        assertTrue(service.isEnabled());
        UUID id = UUID.randomUUID();
        MonitoredServiceRepository serviceRepository = mock(MonitoredServiceRepository.class);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(service));

        new DisableMonitoringCommand(id, serviceRepository).execute();

        assertFalse(service.isEnabled());
        verify(serviceRepository).save(service);
    }

    @Test
    void enable_turnsOnMonitoring() {
        MonitoredService service = newService();
        service.setEnabled(false);
        UUID id = UUID.randomUUID();
        MonitoredServiceRepository serviceRepository = mock(MonitoredServiceRepository.class);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(service));

        new EnableMonitoringCommand(id, serviceRepository).execute();

        assertTrue(service.isEnabled());
        verify(serviceRepository).save(service);
    }
}
