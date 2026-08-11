package com.cloudpulse.command;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

// Turns monitoring off for a service (for maintenance, or to pause it in the demo).
public class DisableMonitoringCommand implements MonitoringCommand {

    private static final Logger log = LoggerFactory.getLogger(DisableMonitoringCommand.class);

    private final UUID serviceId;
    private final MonitoredServiceRepository serviceRepository;

    public DisableMonitoringCommand(UUID serviceId, MonitoredServiceRepository serviceRepository) {
        this.serviceId = serviceId;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void execute() {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
        service.setEnabled(false);
        serviceRepository.save(service);

        log.info("Monitoring disabled for service '{}' ({})", service.getName(), serviceId);
    }
}
