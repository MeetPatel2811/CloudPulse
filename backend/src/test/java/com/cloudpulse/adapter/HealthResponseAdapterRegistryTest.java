package com.cloudpulse.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthResponseAdapterRegistryTest {

    // Deliberately pass Generic first: @Order must still keep it from shadowing JSON adapters.
    private final HealthResponseAdapterRegistry registry = new HealthResponseAdapterRegistry(List.of(
            new GenericHttpAdapter(),
            new LegacyHealthAdapter(),
            new StandardHealthAdapter()
    ));

    @Test
    void standardJsonUsesStandardAdapter() {
        AdaptedHealthResponse response = registry.adapt(
                "{\"status\":\"UP\",\"responseTimeMs\":42}", 200, 75);

        assertEquals(HttpResponseFormat.STANDARD_JSON, response.format());
        assertEquals(42, response.result().getResponseTimeMs());
        assertTrue(response.result().isReachable());
    }

    @Test
    void legacyJsonUsesLegacyAdapter() {
        AdaptedHealthResponse response = registry.adapt(
                "{\"result\":\"ok\",\"latency\":38}", 200, 75);

        assertEquals(HttpResponseFormat.LEGACY_JSON, response.format());
        assertEquals(38, response.result().getResponseTimeMs());
    }

    @Test
    void htmlUsesGenericFallback() {
        AdaptedHealthResponse response = registry.adapt("<html>Notion</html>", 200, 91);

        assertEquals(HttpResponseFormat.GENERIC_HTTP, response.format());
        assertEquals(91, response.result().getResponseTimeMs());
        assertTrue(response.result().isReachable());
    }
}
