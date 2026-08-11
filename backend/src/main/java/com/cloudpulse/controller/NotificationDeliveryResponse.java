package com.cloudpulse.controller;

import com.cloudpulse.model.NotificationDelivery;
import com.cloudpulse.model.WebhookProvider;

import java.time.Instant;
import java.util.UUID;

// API view of one notification delivery attempt (for the delivery-history endpoint).
public record NotificationDeliveryResponse(
        UUID id,
        WebhookProvider provider,
        String targetUrl,
        String serviceName,
        String message,
        boolean success,
        int httpStatus,
        String error,
        Instant sentAt) {

    public static NotificationDeliveryResponse from(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(
                delivery.getId(),
                delivery.getProvider(),
                delivery.getTargetUrl(),
                delivery.getServiceName(),
                delivery.getMessage(),
                delivery.isSuccess(),
                delivery.getHttpStatus(),
                delivery.getError(),
                delivery.getSentAt());
    }
}
