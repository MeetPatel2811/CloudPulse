package com.cloudpulse.controller;

import com.cloudpulse.model.ServiceStatus;

import java.time.Instant;

public record PublicServiceStatusResponse(
        String name,
        String serviceGroup,
        ServiceStatus status,
        Instant lastCheckedAt) {
}

