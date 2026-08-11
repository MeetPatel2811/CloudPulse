package com.cloudpulse.evaluation;

public record HealthCheckSnapshot(
        boolean reachable,
        int httpStatus,
        long responseTimeMs
) {
}
