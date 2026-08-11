package com.cloudpulse.scheduler;

import com.cloudpulse.command.RunHealthCheckCommand;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.ServiceMonitor;

@FunctionalInterface
public interface RunHealthCheckCommandFactory {
    RunHealthCheckCommand create(MonitoredService service, ServiceMonitor monitor);
}