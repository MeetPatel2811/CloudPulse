package com.cloudpulse.scheduler;

import com.cloudpulse.command.RunHealthCheckCommand;
import com.cloudpulse.handler.HealthCheckProcessor;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.stereotype.Component;

@Component
public class SpringRunHealthCheckCommandFactory implements RunHealthCheckCommandFactory {

    private final HealthCheckProcessor processor;
    private final HealthCheckResultRepository resultRepository;
    private final MonitoredServiceRepository serviceRepository;

    public SpringRunHealthCheckCommandFactory(HealthCheckProcessor processor,
                                              HealthCheckResultRepository resultRepository,
                                              MonitoredServiceRepository serviceRepository) {
        this.processor = processor;
        this.resultRepository = resultRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public RunHealthCheckCommand create(MonitoredService service, ServiceMonitor monitor) {
        return new RunHealthCheckCommand(service, monitor, processor, resultRepository, serviceRepository);
    }
}