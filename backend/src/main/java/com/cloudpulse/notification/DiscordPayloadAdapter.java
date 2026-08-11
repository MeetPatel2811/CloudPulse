package com.cloudpulse.notification;

import com.cloudpulse.model.WebhookProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

// Discord webhooks use {"content": "..."} rather than Slack's "text" key.
@Component
public class DiscordPayloadAdapter implements WebhookPayloadAdapter {

    @Override
    public boolean supports(WebhookProvider provider) {
        return provider == WebhookProvider.DISCORD;
    }

    @Override
    public String buildBody(NotificationMessage message) {
        return WebhookJson.write(Map.of("content", message.text()));
    }
}
