package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import java.util.concurrent.ThreadLocalRandom;

public class MockServiceMonitor implements ServiceMonitor {

    private boolean forceReachable = true;
    private int forceHttpStatus = 200;
    private long forceResponseTimeMs = -1;

    public MockServiceMonitor() {
    }

    public MockServiceMonitor(boolean forceReachable, int forceHttpStatus, long forceResponseTimeMs) {
        this.forceReachable = forceReachable;
        this.forceHttpStatus = forceHttpStatus;
        this.forceResponseTimeMs = forceResponseTimeMs;
    }

    public void setForceReachable(boolean forceReachable) { this.forceReachable = forceReachable; }
    public void setForceHttpStatus(int forceHttpStatus) { this.forceHttpStatus = forceHttpStatus; }
    public void setForceResponseTimeMs(long forceResponseTimeMs) { this.forceResponseTimeMs = forceResponseTimeMs; }

    @Override
    public HealthCheckResult check(MonitoredService service) {
        long responseTime = forceResponseTimeMs >= 0
                ? forceResponseTimeMs
                : ThreadLocalRandom.current().nextLong(5, 50);

        HealthCheckResult result = new HealthCheckResult(
                forceReachable, forceHttpStatus, responseTime,
                "mock-response for " + service.getName()
        );
        result.setEvaluatedStatus(forceReachable ? ServiceStatus.HEALTHY : ServiceStatus.DOWN);
        return result;
    }
}