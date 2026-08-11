package com.cloudpulse.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** A null threshold restores the default supplied by the selected evaluation Strategy. */
public record UpdateReliabilityPolicyRequest(
        @Min(100) @Max(5_000) Long latencyThresholdMs,
        @Min(1) @Max(5) Integer alertFailureThreshold) {
}
