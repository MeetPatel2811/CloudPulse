package com.cloudpulse.notification;

import com.cloudpulse.model.WebhookProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

// Slack incoming webhooks accept a simple {"text": "..."} body.
@Component
public class SlackPayloadAdapter implements WebhookPayloadAdapter {

    @Override
    public boolean supports(WebhookProvider provider) {
        return provider == WebhookProvider.SLACK;
    }

    @Override
    public String buildBody(NotificationMessage message) {
        return WebhookJson.write(Map.of("text", message.text()));
    }
}
