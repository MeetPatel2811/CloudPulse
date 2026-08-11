package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.MonitorType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetryMonitorDecoratorTest {

    private final MonitoredService service =
            new MonitoredService("Test Service", "http://localhost:8081/health", MonitorType.MOCK);

    private static class FlakyThenHealthyMonitor implements ServiceMonitor {
        private final int failuresBeforeSuccess;
        private int callCount = 0;

        FlakyThenHealthyMonitor(int failuresBeforeSuccess) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public HealthCheckResult check(MonitoredService service) {
            callCount++;
            boolean reachable = callCount > failuresBeforeSuccess;
            return new HealthCheckResult(reachable, reachable ? 200 : 500, 10, "attempt " + callCount);
        }

        int getCallCount() { return callCount; }
    }

    @Test
    void check_retriesUntilSuccess() {
        FlakyThenHealthyMonitor flaky = new FlakyThenHealthyMonitor(2);
        RetryMonitorDecorator retry = new RetryMonitorDecorator(flaky, 3, 1);
        HealthCheckResult result = retry.check(service);
        assertTrue(result.isReachable());
        assertEquals(3, flaky.getCallCount());
    }

    @Test
    void check_returnsLastFailureWhenAllAttemptsFail() {
        FlakyThenHealthyMonitor alwaysFails = new FlakyThenHealthyMonitor(Integer.MAX_VALUE);
        RetryMonitorDecorator retry = new RetryMonitorDecorator(alwaysFails, 3, 1);
        HealthCheckResult result = retry.check(service);
        assertFalse(result.isReachable());
        assertEquals(3, alwaysFails.getCallCount());
    }

    @Test
    void check_doesNotRetryOnFirstSuccess() {
        MockServiceMonitor healthyMock = new MockServiceMonitor(true, 200, 15);
        RetryMonitorDecorator retry = new RetryMonitorDecorator(healthyMock, 3, 1);
        HealthCheckResult result = retry.check(service);
        assertTrue(result.isReachable());
    }
}
