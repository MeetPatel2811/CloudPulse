package com.cloudpulse.notification;

import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.Severity;

// The provider-neutral content of one alert notification. Each WebhookPayloadAdapter
// turns this into the JSON body its provider expects.
public record NotificationMessage(
        String serviceName,
        ServiceStatus status,
        Severity severity,
        String text) {
}
