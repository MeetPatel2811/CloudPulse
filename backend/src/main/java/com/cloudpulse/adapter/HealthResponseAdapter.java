package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;

public interface HealthResponseAdapter {
    HealthCheckResult adapt(String rawResponse, int httpStatus, long responseTimeMs);
    boolean supports(String rawResponse);
    HttpResponseFormat format();
}
