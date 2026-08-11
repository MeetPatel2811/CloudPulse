package com.cloudpulse.controller;

import java.util.List;
import java.util.UUID;

// A view of one incident's ownership and activity timeline, assembled from its
// IncidentActivity records (owner = author of the latest ASSIGNED entry).
public record IncidentResponse(
        UUID alertId,
        String owner,
        List<IncidentActivityResponse> activity) {
}
