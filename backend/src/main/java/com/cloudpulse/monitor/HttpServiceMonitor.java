package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HttpServiceMonitor implements ServiceMonitor {

    private static final Logger log = LoggerFactory.getLogger(HttpServiceMonitor.class);

    private final HttpMonitoringService monitoringService;

    public HttpServiceMonitor(HttpMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        long start = System.nanoTime();
        try {
            return monitoringService.inspect(service.getHealthUrl()).result();
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("Health check failed for service '{}' at {}: {}",
                    service.getName(), service.getHealthUrl(), e.getMessage());
            return new HealthCheckResult(false, 0, elapsedMs,
                    "Request failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
