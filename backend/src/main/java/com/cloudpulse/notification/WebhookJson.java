package com.cloudpulse.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// Small shared helper so each adapter can turn a map into a JSON string without repeating
// the Jackson boilerplate. Payloads are tiny and fixed-shape, so a single mapper is fine.
final class WebhookJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WebhookJson() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build webhook JSON payload", e);
        }
    }
}
