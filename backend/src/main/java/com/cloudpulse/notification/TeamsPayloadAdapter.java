package com.cloudpulse.notification;

import com.cloudpulse.model.Severity;
import com.cloudpulse.model.WebhookProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

// Microsoft Teams connectors expect a "MessageCard" object, not a plain text key, and
// support a themeColor we set from the alert severity.
@Component
public class TeamsPayloadAdapter implements WebhookPayloadAdapter {

    @Override
    public boolean supports(WebhookProvider provider) {
        return provider == WebhookProvider.TEAMS;
    }

    @Override
    public String buildBody(NotificationMessage message) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("@type", "MessageCard");
        card.put("@context", "http://schema.org/extensions");
        card.put("themeColor", themeColor(message.severity()));
        card.put("text", message.text());
        return WebhookJson.write(card);
    }

    private String themeColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "D0021B"; // red
            case WARNING -> "F5A623";  // amber
            case INFO -> "7ED321";     // green
        };
    }
}
