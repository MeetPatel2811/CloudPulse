package com.cloudpulse.monitor;

import org.springframework.stereotype.Component;

@Component
public class HttpMonitorCreator extends MonitorCreator {

    private final HttpServiceMonitor httpServiceMonitor;

    public HttpMonitorCreator(HttpServiceMonitor httpServiceMonitor) {
        this.httpServiceMonitor = httpServiceMonitor;
    }

    @Override
    public ServiceMonitor createMonitor() {
        return httpServiceMonitor;
    }
}