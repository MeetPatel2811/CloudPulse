package com.cloudpulse.command;

import com.cloudpulse.handler.HealthCheckProcessor;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RunHealthCheckCommandTest {

    @Test
    void execute_runsMonitorProcessesResultAndPersists() {
        MonitoredService service =
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK);
        ServiceMonitor monitor = mock(ServiceMonitor.class);
        HealthCheckProcessor processor = mock(HealthCheckProcessor.class);
        HealthCheckResultRepository resultRepository = mock(HealthCheckResultRepository.class);
        MonitoredServiceRepository serviceRepository = mock(MonitoredServiceRepository.class);

        HealthCheckResult result = new HealthCheckResult(true, 200, 100, "UP");
        when(monitor.check(service)).thenReturn(result);

        new RunHealthCheckCommand(service, monitor, processor, resultRepository, serviceRepository)
                .execute();

        assertEquals(service, result.getService());
        verify(processor).process(result);
        verify(resultRepository).save(result);
        verify(serviceRepository).save(service);
        assertNotNull(service.getLastCheckedAt());
    }
}
