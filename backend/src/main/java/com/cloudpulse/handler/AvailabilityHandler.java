package com.cloudpulse.handler;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.ServiceStatus;

// Step 2: check if the service is available.
// If it could not be reached, or replied with an HTTP error (400 or higher),
// mark it DOWN. We still pass it on so the later steps can raise an alert.
public class AvailabilityHandler extends ResultHandler {

    @Override
    public void handle(HealthCheckResult result) {
        boolean httpError = result.isReachable() && result.getHttpStatus() >= 400;
        if (!result.isReachable() || httpError) {
            result.setEvaluatedStatus(ServiceStatus.DOWN);
        }
        passToNext(result);
    }
}
