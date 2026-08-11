package com.cloudpulse.controller;

import com.cloudpulse.model.WebhookProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Body for registering a webhook target. Validation errors become a 400 via GlobalExceptionHandler.
public record RegisterWebhookRequest(
        @NotNull WebhookProvider provider,
        @NotBlank String url,
        String label) {
}
