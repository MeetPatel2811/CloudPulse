package com.cloudpulse.scheduler;

import com.cloudpulse.command.RunHealthCheckCommand;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.HttpMonitorCreator;
import com.cloudpulse.monitor.KeywordMonitorCreator;
import com.cloudpulse.monitor.LoggingMonitorDecorator;
import com.cloudpulse.monitor.MetricsMonitorDecorator;
import com.cloudpulse.monitor.MockMonitorCreator;
import com.cloudpulse.monitor.MonitorCreator;
import com.cloudpulse.monitor.RetryMonitorDecorator;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@ConditionalOnProperty(prefix = "cloudpulse.scheduler", name = "enabled", havingValue = "true")
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final MonitoredServiceRepository serviceRepository;
    private final HttpMonitorCreator httpMonitorCreator;
    private final MockMonitorCreator mockMonitorCreator;
    private final KeywordMonitorCreator keywordMonitorCreator;
    private final RunHealthCheckCommandFactory commandFactory;
    private final TaskScheduler taskScheduler;

    private final Map<UUID, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

    public HealthCheckScheduler(MonitoredServiceRepository serviceRepository,
                                HttpMonitorCreator httpMonitorCreator,
                                MockMonitorCreator mockMonitorCreator,
                                KeywordMonitorCreator keywordMonitorCreator,
                                RunHealthCheckCommandFactory commandFactory,
                                TaskScheduler taskScheduler) {
        this.serviceRepository = serviceRepository;
        this.httpMonitorCreator = httpMonitorCreator;
        this.mockMonitorCreator = mockMonitorCreator;
        this.keywordMonitorCreator = keywordMonitorCreator;
        this.commandFactory = commandFactory;
        this.taskScheduler = taskScheduler;
    }

    @Scheduled(fixedDelayString = "${cloudpulse.scheduler.sync-delay-ms:5000}")
    public void syncSchedules() {
        List<MonitoredService> allServices = serviceRepository.findAll();
        Map<UUID, MonitoredService> servicesById = new ConcurrentHashMap<>();
        for (MonitoredService service : allServices) {
            servicesById.put(service.getId(), service);
        }

        scheduledTasks.keySet().removeIf(id -> {
            MonitoredService service = servicesById.get(id);
            boolean shouldCancel = service == null || !service.isEnabled();
            if (shouldCancel) {
                cancelTask(id);
            }
            return shouldCancel;
        });

        for (MonitoredService service : allServices) {
            if (!service.isEnabled()) {
                continue;
            }
            scheduleOrReschedule(service);
        }
    }

    private void scheduleOrReschedule(MonitoredService service) {
        UUID id = service.getId();
        long intervalSeconds = service.getCheckIntervalSeconds();
        ScheduledTask existing = scheduledTasks.get(id);

        if (existing != null && existing.intervalSeconds() == intervalSeconds) {
            return;
        }

        if (existing != null) {
            existing.future().cancel(false);
        }

        ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
                () -> runCheck(id), Duration.ofSeconds(intervalSeconds));
        scheduledTasks.put(id, new ScheduledTask(future, intervalSeconds));
        log.debug("Scheduled health check for service {} every {}s", id, intervalSeconds);
    }

    private void cancelTask(UUID id) {
        ScheduledTask task = scheduledTasks.get(id);
        if (task != null) {
            task.future().cancel(false);
            log.debug("Cancelled health check schedule for service {}", id);
        }
    }

    private void runCheck(UUID serviceId) {
        try {
            Optional<MonitoredService> maybeService = serviceRepository.findById(serviceId);
            if (maybeService.isEmpty() || !maybeService.get().isEnabled()) {
                return;
            }
            checkService(maybeService.get());
        } catch (Exception e) {
            log.error("Health check failed for service {}: {}", serviceId, e.getMessage(), e);
        }
    }

    private void checkService(MonitoredService service) {
        ServiceMonitor monitor = createDecoratedMonitor(service);
        RunHealthCheckCommand command = commandFactory.create(service, monitor);
        command.execute();
    }

    private ServiceMonitor createDecoratedMonitor(MonitoredService service) {
        MonitorCreator creator = resolveCreator(service.getMonitorType());
        ServiceMonitor baseMonitor = creator.createMonitor();
        return new MetricsMonitorDecorator(new RetryMonitorDecorator(new LoggingMonitorDecorator(baseMonitor)));
    }

    private MonitorCreator resolveCreator(MonitorType monitorType) {
        return switch (monitorType) {
            case HTTP -> httpMonitorCreator;
            case MOCK -> mockMonitorCreator;
            case KEYWORD -> keywordMonitorCreator;
        };
    }

    private record ScheduledTask(ScheduledFuture<?> future, long intervalSeconds) {
    }
}
