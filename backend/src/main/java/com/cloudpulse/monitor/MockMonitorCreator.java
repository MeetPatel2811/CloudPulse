package com.cloudpulse.monitor;

import org.springframework.stereotype.Component;

@Component
public class MockMonitorCreator extends MonitorCreator {
    @Override
    public ServiceMonitor createMonitor() {
        return new MockServiceMonitor();
    }
}