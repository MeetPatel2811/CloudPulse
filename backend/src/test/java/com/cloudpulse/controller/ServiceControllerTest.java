package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Spring context (H2 database, real repositories) and drives
 * {@link ServiceController} through MockMvc, matching the style of
 * {@code ProcessingWorkflowIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "cloudpulse.scheduler.enabled=false",
        "cloudpulse.http.allowed-hosts=localhost,keyword.example"
})
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private HealthCheckResultRepository resultRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_thenListIncludesIt() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest(
                        "Notification",
                        "http://localhost:8083/health",
                        MonitorType.MOCK,
                        " Platform ",
                        List.of(" API ", "critical", "api")));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Notification"))
                .andExpect(jsonPath("$.currentStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.serviceGroup").value("Platform"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[0]").value("api"))
                .andExpect(jsonPath("$.tags[1]").value("critical"));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Notification')]").exists());
    }

    @Test
    void register_withCheckInterval_appliesIt() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Batch Job", "http://localhost:8085/health", MonitorType.MOCK, 60L));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIntervalSeconds").value(60));
    }

    @Test
    void register_withUnsupportedCheckInterval_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Batch Job", "http://localhost:8085/health", MonitorType.MOCK, 45L));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void update_withCheckInterval_appliesIt() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Reports", "http://localhost:8081/health", MonitorType.MOCK));

        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Reports", "http://localhost:8081/health", MonitorType.MOCK, 300L));

        mockMvc.perform(put("/api/services/{id}", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIntervalSeconds").value(300));
    }

    @Test
    void update_omittingCheckInterval_leavesItUnchanged() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Reports", "http://localhost:8081/health", MonitorType.MOCK));
        service.setCheckIntervalSeconds(60);
        serviceRepository.save(service);

        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Reports v2", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(put("/api/services/{id}", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIntervalSeconds").value(60));
    }

    @Test
    void register_keywordService_withExpectedKeyword_succeeds() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterServiceRequest(
                "Status Page", "https://keyword.example/status", MonitorType.KEYWORD, null, "All Systems Operational"));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitorType").value("KEYWORD"))
                .andExpect(jsonPath("$.expectedKeyword").value("All Systems Operational"));
    }

    @Test
    void register_keywordService_missingKeyword_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterServiceRequest(
                "Status Page", "https://keyword.example/status", MonitorType.KEYWORD,
                null, null, null, List.of()));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("expectedKeyword is required for the KEYWORD monitor type"));
    }

    @Test
    void register_keywordService_blankKeyword_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterServiceRequest(
                "Status Page", "https://keyword.example/status", MonitorType.KEYWORD, null, "   "));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_nonKeywordService_ignoresExpectedKeyword() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterServiceRequest(
                "Notification", "http://localhost:8083/health", MonitorType.MOCK, null, "irrelevant"));

        String response = mockMvc.perform(
                        post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        assertNull(serviceRepository.findById(id).orElseThrow().getExpectedKeyword());
    }

    @Test
    void registerHttpService_rejectsPrivateAddress() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Unsafe", "http://127.0.0.1/internal", MonitorType.HTTP));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("URL resolves to a private or restricted address"));
    }

    @Test
    void runCheck_marksServiceHealthy() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("User", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(post("/api/services/{id}/check", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("HEALTHY"));
    }

    @Test
    void runCheck_unknownService_returns404() throws Exception {
        mockMvc.perform(post("/api/services/{id}/check", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_changesEditableFields() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Billing", "http://localhost:8081/health", MonitorType.MOCK));

        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest(
                        "Billing v2",
                        "http://localhost:8090/health",
                        MonitorType.HTTP,
                        "Revenue",
                        List.of("payments", "tier-1")));

        mockMvc.perform(put("/api/services/{id}", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Billing v2"))
                .andExpect(jsonPath("$.healthUrl").value("http://localhost:8090/health"))
                .andExpect(jsonPath("$.monitorType").value("HTTP"))
                .andExpect(jsonPath("$.serviceGroup").value("Revenue"))
                .andExpect(jsonPath("$.tags[0]").value("payments"))
                .andExpect(jsonPath("$.tags[1]").value("tier-1"));
    }

    @Test
    void register_rejectsMoreThanTenTags() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterServiceRequest(
                "Too many tags",
                "http://localhost:8083/health",
                MonitorType.MOCK,
                "Platform",
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_unknownService_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterServiceRequest("Ghost", "http://localhost:8091/health", MonitorType.MOCK));

        mockMvc.perform(put("/api/services/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_removesService() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Temp", "http://localhost:8082/health", MonitorType.MOCK));

        mockMvc.perform(delete("/api/services/{id}", service.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/services/{id}", service.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_unknownService_returns404() throws Exception {
        // With the local handler removed, 404s now come back as the shared JSON ErrorResponse
        // (via GlobalExceptionHandler), not a plain-text body.
        mockMvc.perform(delete("/api/services/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void delete_serviceWithHistory_returns409AndKeepsService() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Analytics", "http://localhost:8084/health", MonitorType.MOCK));
        HealthCheckResult result = new HealthCheckResult(true, 200, 120, "seed");
        result.setService(service);
        resultRepository.save(result);

        mockMvc.perform(delete("/api/services/{id}", service.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a service with monitoring history. Disable it instead."));

        // The service must NOT have been deleted.
        mockMvc.perform(get("/api/services/{id}", service.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void disable_thenEnable_roundTrips() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8082/health", MonitorType.MOCK));

        mockMvc.perform(post("/api/services/{id}/disable", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/services/{id}/enable", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
