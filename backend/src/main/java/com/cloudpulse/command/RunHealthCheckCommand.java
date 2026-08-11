package com.cloudpulse.command;

import com.cloudpulse.handler.HealthCheckProcessor;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

// Runs one health check for a service:
//   1. ask the monitor to check it
//   2. send the result through the processing chain (status change + alerts)
//   3. save the result and update when the service was last checked
// This is what the scheduler or a "run check now" button will call.
public class RunHealthCheckCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(RunHealthCheckCommand.class);

    private final MonitoredService service;
    private final ServiceMonitor monitor;
    private final HealthCheckProcessor processor;
    private final HealthCheckResultRepository resultRepository;
    private final MonitoredServiceRepository serviceRepository;

    public RunHealthCheckCommand(MonitoredService service,
                                 ServiceMonitor monitor,
                                 HealthCheckProcessor processor,
                                 HealthCheckResultRepository resultRepository,
                                 MonitoredServiceRepository serviceRepository) {
        this.service = service;
        this.monitor = monitor;
        this.processor = processor;
        this.resultRepository = resultRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void execute() {
        HealthCheckResult result = monitor.check(service);
        result.setService(service);

        // Run the result through the processing chain (status change + alerts).
        processor.process(result);

        // Save the result and update the last-checked time.
        resultRepository.save(result);
        service.setLastCheckedAt(Instant.now());
        serviceRepository.save(service);

        log.debug("Ran health check for '{}': reachable={}, status={}",
                service.getName(), result.isReachable(), result.getEvaluatedStatus());
    }
}
