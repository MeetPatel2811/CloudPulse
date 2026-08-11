package com.cloudpulse.controller;

import java.time.Instant;
import java.util.List;

public record PublicStatusResponse(
        Instant generatedAt,
        String overallStatus,
        List<PublicServiceStatusResponse> services) {
}

