package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.MonitorType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingMonitorDecoratorTest {
    @Test
    void check_doesNotAlterDelegateResult() {
        MockServiceMonitor mock = new MockServiceMonitor(true, 200, 33);
        LoggingMonitorDecorator decorator = new LoggingMonitorDecorator(mock);
        MonitoredService service = new MonitoredService("Test Service", "http://localhost:8081/health", MonitorType.MOCK);
        HealthCheckResult result = decorator.check(service);
        assertTrue(result.isReachable());
        assertEquals(200, result.getHttpStatus());
        assertEquals(33, result.getResponseTimeMs());
    }
}