package com.cloudpulse.notification;

import com.cloudpulse.model.WebhookProvider;

// Adapter pattern: Slack, Teams, and Discord each expect a differently shaped JSON body
// for the same notification. An adapter declares which provider it handles and converts a
// NotificationMessage into that provider's request body, so the notifier stays provider-agnostic.
public interface WebhookPayloadAdapter {

    boolean supports(WebhookProvider provider);

    String buildBody(NotificationMessage message);
}
