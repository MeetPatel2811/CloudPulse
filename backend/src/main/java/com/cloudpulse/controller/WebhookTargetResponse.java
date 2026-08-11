package com.cloudpulse.controller;

import com.cloudpulse.model.WebhookProvider;
import com.cloudpulse.model.WebhookTarget;

import java.time.Instant;
import java.util.UUID;

// API view of a registered webhook target.
public record WebhookTargetResponse(
        UUID id,
        WebhookProvider provider,
        String url,
        String label,
        boolean enabled,
        Instant createdAt) {

    public static WebhookTargetResponse from(WebhookTarget target) {
        return new WebhookTargetResponse(
                target.getId(),
                target.getProvider(),
                target.getUrl(),
                target.getLabel(),
                target.isEnabled(),
                target.getCreatedAt());
    }
}
