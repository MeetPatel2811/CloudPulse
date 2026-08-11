package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// A chat destination that alert notifications are posted to (e.g. a Slack incoming
// webhook URL). Operators register these; the WebhookNotifier sends to every enabled one.
@Entity
@Table(name = "webhook_target")
public class WebhookTarget {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookProvider provider;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column
    private String label;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WebhookTarget() {
    }

    public WebhookTarget(WebhookProvider provider, String url, String label) {
        this.provider = provider;
        this.url = url;
        this.label = label;
    }

    public UUID getId() { return id; }
    public WebhookProvider getProvider() { return provider; }
    public void setProvider(WebhookProvider provider) { this.provider = provider; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
