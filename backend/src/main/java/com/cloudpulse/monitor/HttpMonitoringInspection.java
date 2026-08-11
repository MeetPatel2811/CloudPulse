package com.cloudpulse.monitor;

import com.cloudpulse.adapter.HttpResponseFormat;
import com.cloudpulse.model.HealthCheckResult;

import java.net.URI;

/** Normalized result and safe transport metadata used by checks and probe previews. */
public record HttpMonitoringInspection(
        HealthCheckResult result,
        HttpResponseFormat responseFormat,
        URI finalUri,
        int redirectCount,
        boolean bodyTruncated
) {
}
