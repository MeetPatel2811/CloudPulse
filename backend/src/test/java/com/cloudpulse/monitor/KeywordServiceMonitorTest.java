package com.cloudpulse.monitor;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.http.HttpMonitoringProperties;
import com.cloudpulse.monitor.http.HttpTransport;
import com.cloudpulse.monitor.http.HttpTransportResponse;
import com.cloudpulse.monitor.http.SafeHttpFetcher;
import com.cloudpulse.monitor.http.SafeUrlValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordServiceMonitorTest {

    private MonitoredService service(String keyword) {
        MonitoredService svc = new MonitoredService("Status Page", "https://public.example/status", MonitorType.KEYWORD);
        svc.setExpectedKeyword(keyword);
        return svc;
    }

    private KeywordServiceMonitor monitorWithBody(String body) {
        HttpMonitoringProperties properties = new HttpMonitoringProperties();
        properties.setAllowedHosts(List.of("public.example"));
        HttpTransport transport = uri -> new HttpTransportResponse(200, body, false, null);
        SafeHttpFetcher fetcher = new SafeHttpFetcher(new SafeUrlValidator(properties), transport, properties);
        return new KeywordServiceMonitor(fetcher);
    }

    @Test
    void keywordPresent_isReachable() {
        KeywordServiceMonitor monitor = monitorWithBody("<html>All Systems Operational</html>");

        HealthCheckResult result = monitor.check(service("All Systems Operational"));

        assertTrue(result.isReachable());
        assertTrue(result.getRawMessage().contains("found"));
    }

    @Test
    void keywordMissing_isNotReachable() {
        KeywordServiceMonitor monitor = monitorWithBody("<html>Service Disruption</html>");

        HealthCheckResult result = monitor.check(service("All Systems Operational"));

        assertFalse(result.isReachable());
        assertTrue(result.getRawMessage().contains("not found"));
    }

    @Test
    void fetchFailure_isNotReachable() {
        HttpMonitoringProperties properties = new HttpMonitoringProperties();
        properties.setAllowedHosts(List.of("public.example"));
        HttpTransport transport = uri -> {
            throw new IOException("connection refused");
        };
        SafeHttpFetcher fetcher = new SafeHttpFetcher(new SafeUrlValidator(properties), transport, properties);
        KeywordServiceMonitor monitor = new KeywordServiceMonitor(fetcher);

        HealthCheckResult result = monitor.check(service("All Systems Operational"));

        assertFalse(result.isReachable());
    }
}
