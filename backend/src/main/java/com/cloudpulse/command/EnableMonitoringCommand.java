package com.cloudpulse.command;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

// Turns monitoring on for a service so the scheduler checks it again.
public class EnableMonitoringCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(EnableMonitoringCommand.class);

    private final UUID serviceId;
    private final MonitoredServiceRepository serviceRepository;

    public EnableMonitoringCommand(UUID serviceId, MonitoredServiceRepository serviceRepository) {
        this.serviceId = serviceId;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void execute() {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
        service.setEnabled(true);
        serviceRepository.save(service);

        log.info("Monitoring enabled for service '{}' ({})", service.getName(), serviceId);
    }
}
