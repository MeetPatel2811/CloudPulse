package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsMonitorDecorator extends MonitorDecorator {

    private static final Logger log = LoggerFactory.getLogger(MetricsMonitorDecorator.class);

    public MetricsMonitorDecorator(ServiceMonitor delegate) {
        super(delegate);
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        long start = System.nanoTime();
        HealthCheckResult result = delegate.check(service);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("[metrics] check() for service '{}' took {}ms wall-clock", service.getName(), elapsedMs);
        return result;
    }
}