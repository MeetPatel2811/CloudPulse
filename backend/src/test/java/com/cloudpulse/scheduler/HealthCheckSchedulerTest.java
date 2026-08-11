package com.cloudpulse.scheduler;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.HttpMonitorCreator;
import com.cloudpulse.monitor.KeywordMonitorCreator;
import com.cloudpulse.monitor.MockMonitorCreator;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthCheckSchedulerTest {

    private MonitoredServiceRepository serviceRepository;
    private HttpMonitorCreator httpMonitorCreator;
    private MockMonitorCreator mockMonitorCreator;
    private KeywordMonitorCreator keywordMonitorCreator;
    private RunHealthCheckCommandFactory commandFactory;
    private TaskScheduler taskScheduler;
    private HealthCheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        serviceRepository = mock(MonitoredServiceRepository.class);
        httpMonitorCreator = mock(HttpMonitorCreator.class);
        mockMonitorCreator = mock(MockMonitorCreator.class);
        keywordMonitorCreator = mock(KeywordMonitorCreator.class);
        commandFactory = mock(RunHealthCheckCommandFactory.class);
        taskScheduler = mock(TaskScheduler.class);

        when(mockMonitorCreator.createMonitor()).thenReturn(mock(ServiceMonitor.class));
        when(httpMonitorCreator.createMonitor()).thenReturn(mock(ServiceMonitor.class));

        when(taskScheduler.scheduleWithFixedDelay(any(Runnable.class), any(Duration.class)))
                .thenAnswer(invocation -> mock(ScheduledFuture.class));

        scheduler = new HealthCheckScheduler(
                serviceRepository, httpMonitorCreator, mockMonitorCreator, keywordMonitorCreator,
                commandFactory, taskScheduler);
    }

    private MonitoredService service(String name, boolean enabled, long intervalSeconds) throws Exception {
        MonitoredService svc = new MonitoredService(name, "http://localhost:8081/health", MonitorType.MOCK);
        setId(svc, UUID.randomUUID());
        svc.setEnabled(enabled);
        svc.setCheckIntervalSeconds(intervalSeconds);
        return svc;
    }

    private void setId(MonitoredService service, UUID id) throws Exception {
        Field idField = MonitoredService.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(service, id);
    }

    @Test
    void syncSchedules_schedulesEachEnabledServiceWithItsOwnInterval() throws Exception {
        MonitoredService fast = service("Payment", true, 10);
        MonitoredService slow = service("Auth", true, 30);
        when(serviceRepository.findAll()).thenReturn(List.of(fast, slow));

        scheduler.syncSchedules();

        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(30)));
        verify(taskScheduler, times(2)).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
    }

    @Test
    void syncSchedules_isIdempotentWhenNothingChanged() throws Exception {
        MonitoredService svc = service("Payment", true, 10);
        when(serviceRepository.findAll()).thenReturn(List.of(svc));

        scheduler.syncSchedules();
        scheduler.syncSchedules();

        verify(taskScheduler, times(1)).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
    }

    @Test
    void syncSchedules_reschedulesWhenIntervalChanges() throws Exception {
        MonitoredService svc = service("Payment", true, 10);
        when(serviceRepository.findAll()).thenReturn(List.of(svc));

        scheduler.syncSchedules();

        ScheduledFuture<?> firstFuture = getScheduledFuture(svc.getId());

        svc.setCheckIntervalSeconds(60);
        scheduler.syncSchedules();

        verify(firstFuture).cancel(false);
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(60)));
    }

    @Test
    void syncSchedules_cancelsDisabledServiceAndReschedulesWhenReEnabled() throws Exception {
        MonitoredService svc = service("Payment", true, 10);
        when(serviceRepository.findAll()).thenReturn(List.of(svc));

        scheduler.syncSchedules();
        ScheduledFuture<?> firstFuture = getScheduledFuture(svc.getId());

        svc.setEnabled(false);
        scheduler.syncSchedules();

        verify(firstFuture).cancel(false);

        svc.setEnabled(true);
        scheduler.syncSchedules();

        verify(taskScheduler, times(2)).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
    }

    @SuppressWarnings("unchecked")
    private ScheduledFuture<?> getScheduledFuture(UUID serviceId) throws Exception {
        Field mapField = HealthCheckScheduler.class.getDeclaredField("scheduledTasks");
        mapField.setAccessible(true);
        Map<UUID, Object> scheduledTasks = (Map<UUID, Object>) mapField.get(scheduler);
        Object scheduledTask = scheduledTasks.get(serviceId);
        assertTrue(scheduledTask != null, "expected a scheduled task for service " + serviceId);
        Field futureField = scheduledTask.getClass().getDeclaredField("future");
        futureField.setAccessible(true);
        return (ScheduledFuture<?>) futureField.get(scheduledTask);
    }
}
