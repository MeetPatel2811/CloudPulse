package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyHealthAdapterTest {

    private final LegacyHealthAdapter adapter = new LegacyHealthAdapter();

    @Test
    void adapt_parsesOkResultAsReachable() {
        String json = "{\"result\":\"ok\",\"latency\":37}";
        HealthCheckResult result = adapter.adapt(json, 200, 60);
        assertTrue(result.isReachable());
        assertEquals(37, result.getResponseTimeMs());
    }

    @Test
    void adapt_parsesFailResultAsUnreachable() {
        String json = "{\"result\":\"fail\",\"latency\":0}";
        HealthCheckResult result = adapter.adapt(json, 200, 10);
        assertFalse(result.isReachable());
    }

    @Test
    void adapt_producesEquivalentResultShapeToStandardAdapter() {
        StandardHealthAdapter standardAdapter = new StandardHealthAdapter();
        HealthCheckResult fromLegacy = adapter.adapt("{\"result\":\"ok\",\"latency\":30}", 200, 30);
        HealthCheckResult fromStandard = standardAdapter.adapt("{\"status\":\"UP\",\"responseTimeMs\":30}", 200, 30);
        assertEquals(fromStandard.isReachable(), fromLegacy.isReachable());
        assertEquals(fromStandard.getResponseTimeMs(), fromLegacy.getResponseTimeMs());
    }

    @Test
    void supports_trueForLegacyShape() {
        assertTrue(adapter.supports("{\"result\":\"ok\",\"latency\":42}"));
    }

    @Test
    void supports_falseForStandardShape() {
        assertFalse(adapter.supports("{\"status\":\"UP\",\"responseTimeMs\":42}"));
    }

    @Test
    void format_isLegacyJson() {
        assertEquals(HttpResponseFormat.LEGACY_JSON, adapter.format());
    }
}
