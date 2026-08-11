package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StandardHealthAdapterTest {

    private final StandardHealthAdapter adapter = new StandardHealthAdapter();

    @Test
    void adapt_parsesUpStatusAsReachable() {
        String json = "{\"status\":\"UP\",\"responseTimeMs\":42}";
        HealthCheckResult result = adapter.adapt(json, 200, 55);
        assertTrue(result.isReachable());
        assertEquals(200, result.getHttpStatus());
        assertEquals(42, result.getResponseTimeMs());
    }

    @Test
    void adapt_parsesDownStatusAsUnreachable() {
        String json = "{\"status\":\"DOWN\",\"responseTimeMs\":0}";
        HealthCheckResult result = adapter.adapt(json, 200, 10);
        assertFalse(result.isReachable());
    }

    @Test
    void adapt_httpErrorStatusOverridesReachability() {
        String json = "{\"status\":\"UP\",\"responseTimeMs\":42}";
        HealthCheckResult result = adapter.adapt(json, 500, 10);
        assertFalse(result.isReachable());
    }

    @Test
    void adapt_malformedBodyIsUnreachableNotAnException() {
        String malformed = "not json at all";
        HealthCheckResult result = assertDoesNotThrow(() -> adapter.adapt(malformed, 200, 10));
        assertFalse(result.isReachable());
    }

    @Test
    void supports_trueForStandardShape() {
        assertTrue(adapter.supports("{\"status\":\"UP\",\"responseTimeMs\":42}"));
    }

    @Test
    void supports_falseForLegacyShape() {
        assertFalse(adapter.supports("{\"result\":\"ok\",\"latency\":42}"));
    }

    @Test
    void format_isStandardJson() {
        assertEquals(HttpResponseFormat.STANDARD_JSON, adapter.format());
    }
}
