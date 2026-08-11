package com.cloudpulse.adapter;

import com.cloudpulse.model.HealthCheckResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericHttpAdapterTest {

    private final GenericHttpAdapter adapter = new GenericHttpAdapter();

    @Test
    void ordinaryHtmlWithSuccessfulStatusIsReachable() {
        HealthCheckResult result = adapter.adapt("<html>ok</html>", 200, 125);

        assertTrue(result.isReachable());
        assertEquals(200, result.getHttpStatus());
        assertEquals(125, result.getResponseTimeMs());
        assertEquals(HttpResponseFormat.GENERIC_HTTP, adapter.format());
    }

    @Test
    void serverErrorIsUnreachable() {
        HealthCheckResult result = adapter.adapt("failure", 503, 45);

        assertFalse(result.isReachable());
        assertEquals(503, result.getHttpStatus());
    }
}
