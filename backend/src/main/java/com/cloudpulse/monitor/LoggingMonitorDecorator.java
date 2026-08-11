package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMonitorDecorator extends MonitorDecorator {

    private static final Logger log = LoggerFactory.getLogger(LoggingMonitorDecorator.class);

    public LoggingMonitorDecorator(ServiceMonitor delegate) {
        super(delegate);
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        log.info("Starting health check for service '{}' ({})", service.getName(), service.getHealthUrl());
        HealthCheckResult result = delegate.check(service);
        log.info("Completed health check for service '{}': reachable={}, status={}, responseTimeMs={}",
                service.getName(), result.isReachable(), result.getHttpStatus(), result.getResponseTimeMs());
        return result;
    }
}