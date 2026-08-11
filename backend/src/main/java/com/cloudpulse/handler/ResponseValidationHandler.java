package com.cloudpulse.handler;

import com.cloudpulse.model.HealthCheckResult;

import java.time.Instant;

// Step 1: make sure we have something we can process.
// If there is no result, or no service attached to it, stop the chain.
public class ResponseValidationHandler extends ResultHandler {

    @Override
    public void handle(HealthCheckResult result) {
        if (result == null || result.getService() == null) {
            return; // nothing to process
        }
        if (result.getCheckedAt() == null) {
            result.setCheckedAt(Instant.now());
        }
        passToNext(result);
    }
}
