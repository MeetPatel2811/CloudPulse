package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;

/** A normalized health result together with the Adapter that recognized its format. */
public record AdaptedHealthResponse(
        HealthCheckResult result,
        HttpResponseFormat format
) {
}
