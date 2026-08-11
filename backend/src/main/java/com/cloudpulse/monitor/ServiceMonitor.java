package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;

public interface ServiceMonitor {
    HealthCheckResult check(MonitoredService service);
}