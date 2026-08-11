package com.cloudpulse.monitor;

import org.springframework.stereotype.Component;

@Component
public class KeywordMonitorCreator extends MonitorCreator {

    private final KeywordServiceMonitor keywordServiceMonitor;

    public KeywordMonitorCreator(KeywordServiceMonitor keywordServiceMonitor) {
        this.keywordServiceMonitor = keywordServiceMonitor;
    }

    @Override
    public ServiceMonitor createMonitor() {
        return keywordServiceMonitor;
    }
}
