package com.cloudpulse.handler;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.ServiceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AvailabilityHandlerTest {

    private final AvailabilityHandler handler = new AvailabilityHandler();

    @Test
    void unreachable_isDown() {
        HealthCheckResult result = new HealthCheckResult(false, 0, 0, "timeout");
        handler.handle(result);
        assertEquals(ServiceStatus.DOWN, result.getEvaluatedStatus());
    }

    @Test
    void reachableWithHttpError_isDown() {
        HealthCheckResult result = new HealthCheckResult(true, 503, 50, "server error");
        handler.handle(result);
        assertEquals(ServiceStatus.DOWN, result.getEvaluatedStatus());
    }

    @Test
    void reachableWithOkStatus_isNotDown() {
        HealthCheckResult result = new HealthCheckResult(true, 200, 50, "UP");
        handler.handle(result);
        assertNotEquals(ServiceStatus.DOWN, result.getEvaluatedStatus());
    }
}
