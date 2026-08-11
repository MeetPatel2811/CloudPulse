package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// A record of one attempt to post a notification to a webhook target. We snapshot the
// provider and URL (rather than a foreign key) so history survives if the target is
// later deleted, and we keep whether it succeeded plus the HTTP status / error.
@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookProvider provider;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    // The alert this notification was about, kept as a plain id (alerts can be deleted).
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "http_status")
    private int httpStatus;

    @Column(length = 1000)
    private String error;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    protected NotificationDelivery() {
    }

    public NotificationDelivery(WebhookProvider provider, String targetUrl, UUID alertId,
                                String serviceName, String message,
                                boolean success, int httpStatus, String error) {
        this.provider = provider;
        this.targetUrl = targetUrl;
        this.alertId = alertId;
        this.serviceName = serviceName;
        this.message = message;
        this.success = success;
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public UUID getId() { return id; }
    public WebhookProvider getProvider() { return provider; }
    public String getTargetUrl() { return targetUrl; }
    public UUID getAlertId() { return alertId; }
    public String getServiceName() { return serviceName; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public int getHttpStatus() { return httpStatus; }
    public String getError() { return error; }
    public Instant getSentAt() { return sentAt; }
}
