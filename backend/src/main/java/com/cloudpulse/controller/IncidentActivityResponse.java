package com.cloudpulse.controller;

import com.cloudpulse.model.IncidentActivity;
import com.cloudpulse.model.IncidentActivityType;

import java.time.Instant;
import java.util.UUID;

public record IncidentActivityResponse(
        UUID id,
        IncidentActivityType type,
        String author,
        String detail,
        Instant createdAt) {

    public static IncidentActivityResponse from(IncidentActivity activity) {
        return new IncidentActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getAuthor(),
                activity.getDetail(),
                activity.getCreatedAt());
    }
}
