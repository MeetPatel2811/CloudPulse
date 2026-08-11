package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class LegacyHealthAdapter implements HealthResponseAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public HealthCheckResult adapt(String rawResponse, int httpStatus, long responseTimeMs) {
        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            String result = node.path("result").asText("fail");
            boolean reachable = "ok".equalsIgnoreCase(result) && httpStatus < 400;
            long reportedTime = node.path("latency").asLong(responseTimeMs);
            return new HealthCheckResult(reachable, httpStatus, reportedTime, rawResponse);
        } catch (Exception e) {
            return new HealthCheckResult(false, httpStatus, responseTimeMs,
                    "Failed to parse legacy response: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String rawResponse) {
        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            return node.has("result");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public HttpResponseFormat format() {
        return HttpResponseFormat.LEGACY_JSON;
    }
}
