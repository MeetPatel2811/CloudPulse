package com.cloudpulse.model;

// The chat providers we can post alert notifications to. Each one expects a
// different JSON body, which is what the webhook payload adapters handle.
public enum WebhookProvider {
    SLACK,
    TEAMS,
    DISCORD
}
