package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;

public abstract class MonitorDecorator implements ServiceMonitor {

    protected final ServiceMonitor delegate;

    protected MonitorDecorator(ServiceMonitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        return delegate.check(service);
    }
}