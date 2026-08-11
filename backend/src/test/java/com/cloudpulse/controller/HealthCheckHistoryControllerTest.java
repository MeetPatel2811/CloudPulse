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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class HealthCheckHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private HealthCheckResultRepository resultRepository;

    @Test
    void listForService_returnsResultsNewestFirst() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Inventory", "http://localhost:8083/health", MonitorType.HTTP));

        saveResult(service, Instant.parse("2026-08-02T12:00:00Z"), 45, ServiceStatus.HEALTHY);
        saveResult(service, Instant.parse("2026-08-02T12:01:00Z"), 1_500, ServiceStatus.DEGRADED);

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serviceId").value(service.getId().toString()))
                .andExpect(jsonPath("$[0].serviceName").value("Inventory"))
                .andExpect(jsonPath("$[0].responseTimeMs").value(1_500))
                .andExpect(jsonPath("$[0].evaluatedStatus").value("DEGRADED"))
                .andExpect(jsonPath("$[1].responseTimeMs").value(45))
                .andExpect(jsonPath("$[1].evaluatedStatus").value("HEALTHY"));
    }

    @Test
    void listForService_existingServiceWithoutResults_returnsEmptyList() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Empty", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listForService_unknownService_returns404ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/services/{id}/health-checks", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void listForService_malformedServiceId_returnsConsistent400ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/services/{id}/health-checks", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void listForService_appliesTimeRangeAndLimit() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Timeline", "http://localhost:8081/health", MonitorType.MOCK));

        saveResult(service, Instant.parse("2026-08-02T10:00:00Z"), 50, ServiceStatus.HEALTHY);
        saveResult(service, Instant.parse("2026-08-02T11:00:00Z"), 100, ServiceStatus.HEALTHY);
        saveResult(service, Instant.parse("2026-08-02T12:00:00Z"), 900, ServiceStatus.DEGRADED);

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId())
                        .param("from", "2026-08-02T10:30:00Z")
                        .param("to", "2026-08-02T12:30:00Z")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].responseTimeMs").value(900));
    }

    @Test
    void listForService_invalidQueryParameters_return400() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Invalid query", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId())
                        .param("limit", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit must be between 1 and 1000"));

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId())
                        .param("from", "2026-08-03T00:00:00Z")
                        .param("to", "2026-08-02T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before or equal to to"));

        mockMvc.perform(get("/api/services/{id}/health-checks", service.getId())
                        .param("from", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private void saveResult(
            MonitoredService service,
            Instant checkedAt,
            long responseTimeMs,
            ServiceStatus status) {
        HealthCheckResult result = new HealthCheckResult(true, 200, responseTimeMs, "test response");
        result.setService(service);
        result.setCheckedAt(checkedAt);
        result.setEvaluatedStatus(status);
        resultRepository.save(result);
    }
}
