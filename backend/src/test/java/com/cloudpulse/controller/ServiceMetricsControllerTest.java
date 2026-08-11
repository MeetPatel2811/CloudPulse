package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class ServiceMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private HealthCheckResultRepository resultRepository;

    @Test
    void getMetrics_returnsPortableWindowedSummaryAndP95() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Checkout", "http://localhost:8081/health", MonitorType.MOCK));
        service.setAvailabilitySloPercent(99.0);
        serviceRepository.save(service);
        Instant now = Instant.now();

        saveResult(service, now.minus(30, ChronoUnit.MINUTES), true, 100, ServiceStatus.HEALTHY);
        saveResult(service, now.minus(20, ChronoUnit.MINUTES), true, 200, ServiceStatus.HEALTHY);
        saveResult(service, now.minus(10, ChronoUnit.MINUTES), true, 900, ServiceStatus.DEGRADED);
        saveResult(service, now.minus(5, ChronoUnit.MINUTES), false, 0, ServiceStatus.DOWN);
        saveResult(service, now.minus(2, ChronoUnit.DAYS), true, 5_000, ServiceStatus.DEGRADED);

        mockMvc.perform(get("/api/services/{id}/metrics", service.getId())
                        .param("window", "1h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(service.getId().toString()))
                .andExpect(jsonPath("$.serviceName").value("Checkout"))
                .andExpect(jsonPath("$.window").value("1h"))
                .andExpect(jsonPath("$.windowStart").exists())
                .andExpect(jsonPath("$.windowEnd").exists())
                .andExpect(jsonPath("$.currentStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.activeEvaluationStrategy").value("NORMAL"))
                .andExpect(jsonPath("$.totalChecks").value(4))
                .andExpect(jsonPath("$.successfulChecks").value(3))
                .andExpect(jsonPath("$.failedChecks").value(1))
                .andExpect(jsonPath("$.availabilityPercent").value(75.0))
                .andExpect(jsonPath("$.availabilitySloPercent").value(99.0))
                .andExpect(jsonPath("$.errorBudgetRemainingPercent").value(0.0))
                .andExpect(jsonPath("$.sloMet").value(false))
                .andExpect(jsonPath("$.averageResponseTimeMs").value(400.0))
                .andExpect(jsonPath("$.p95ResponseTimeMs").value(900))
                .andExpect(jsonPath("$.p95SampleSize").value(3))
                .andExpect(jsonPath("$.p95Sampled").value(false));
    }

    @Test
    void getMetrics_serviceWithoutChecks_returnsHonestEmptySummary() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("New service", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(get("/api/services/{id}/metrics", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("24h"))
                .andExpect(jsonPath("$.totalChecks").value(0))
                .andExpect(jsonPath("$.availabilityPercent").value(0.0))
                .andExpect(jsonPath("$.availabilitySloPercent").value(99.0))
                .andExpect(jsonPath("$.errorBudgetRemainingPercent").value(100.0))
                .andExpect(jsonPath("$.sloMet").value(true))
                .andExpect(jsonPath("$.averageResponseTimeMs").doesNotExist())
                .andExpect(jsonPath("$.p95ResponseTimeMs").doesNotExist())
                .andExpect(jsonPath("$.p95SampleSize").value(0))
                .andExpect(jsonPath("$.p95Sampled").value(false));
    }

    @Test
    void getMetrics_rejectsInvalidWindowAndUnknownService() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Window", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(get("/api/services/{id}/metrics", service.getId())
                        .param("window", "30d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("window must be one of: 1h, 6h, 24h, 7d"));

        mockMvc.perform(get("/api/services/{id}/metrics", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private void saveResult(
            MonitoredService service,
            Instant checkedAt,
            boolean reachable,
            long responseTimeMs,
            ServiceStatus status) {
        HealthCheckResult result = new HealthCheckResult(
                reachable,
                reachable ? 200 : 503,
                responseTimeMs,
                "metrics test");
        result.setService(service);
        result.setCheckedAt(checkedAt);
        result.setEvaluatedStatus(status);
        resultRepository.save(result);
    }
}
