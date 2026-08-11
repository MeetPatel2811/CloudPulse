package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryMonitorDecorator extends MonitorDecorator {

    private static final Logger log = LoggerFactory.getLogger(RetryMonitorDecorator.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_DELAY_MS = 200;

    private final int maxAttempts;
    private final long delayMs;

    public RetryMonitorDecorator(ServiceMonitor delegate) {
        this(delegate, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
    }

    public RetryMonitorDecorator(ServiceMonitor delegate, int maxAttempts, long delayMs) {
        super(delegate);
        this.maxAttempts = maxAttempts;
        this.delayMs = delayMs;
    }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        HealthCheckResult lastResult = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            lastResult = delegate.check(service);
            if (lastResult.isReachable()) {
                return lastResult;
            }
            log.warn("Attempt {}/{} unreachable for service '{}'", attempt, maxAttempts, service.getName());
            if (attempt < maxAttempts) {
                sleep(delayMs);
            }
        }
        return lastResult;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}