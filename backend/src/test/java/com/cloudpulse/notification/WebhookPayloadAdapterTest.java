package com.cloudpulse.notification;

import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.Severity;
import com.cloudpulse.model.WebhookProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookPayloadAdapterTest {

    private final NotificationMessage message = new NotificationMessage(
            "Payment", ServiceStatus.DOWN, Severity.CRITICAL, "[CRITICAL] Payment is DOWN (was HEALTHY)");

    @Test
    void slackUsesTextKey() {
        String body = new SlackPayloadAdapter().buildBody(message);
        assertTrue(body.contains("\"text\""));
        assertTrue(body.contains("Payment is DOWN"));
    }

    @Test
    void discordUsesContentKey() {
        String body = new DiscordPayloadAdapter().buildBody(message);
        assertTrue(body.contains("\"content\""));
        assertFalse(body.contains("\"text\""));
    }

    @Test
    void teamsUsesMessageCardWithSeverityColor() {
        String body = new TeamsPayloadAdapter().buildBody(message);
        assertTrue(body.contains("MessageCard"));
        assertTrue(body.contains("themeColor"));
        assertTrue(body.contains("D0021B")); // CRITICAL -> red
    }

    @Test
    void supportsOnlyItsOwnProvider() {
        assertTrue(new SlackPayloadAdapter().supports(WebhookProvider.SLACK));
        assertFalse(new SlackPayloadAdapter().supports(WebhookProvider.DISCORD));
        assertTrue(new DiscordPayloadAdapter().supports(WebhookProvider.DISCORD));
        assertTrue(new TeamsPayloadAdapter().supports(WebhookProvider.TEAMS));
    }
}
