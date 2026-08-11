package com.cloudpulse.controller;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.Severity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Spring context (H2 database, real repositories) and drives
 * {@link AlertController} through MockMvc, matching the style of
 * {@code ProcessingWorkflowIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private AlertRepository alertRepository;

    @Test
    void listAlerts_includesSeededAlert() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8082/health", MonitorType.MOCK));
        Alert alert = alertRepository.save(new Alert(service, Severity.CRITICAL, "Payment is DOWN"));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + alert.getId() + "')]").exists());
    }

    @Test
    void acknowledge_thenResolve_roundTrips() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("User", "http://localhost:8081/health", MonitorType.MOCK));
        Alert alert = alertRepository.save(new Alert(service, Severity.WARNING, "User service is DEGRADED"));

        mockMvc.perform(post("/api/alerts/{id}/acknowledge", alert.getId()).param("acknowledgedBy", "pavithra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedBy").value("pavithra"));

        mockMvc.perform(post("/api/alerts/{id}/resolve", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void acknowledge_unknownAlert_returns404() throws Exception {
        // 404 now comes back as the shared JSON ErrorResponse (via GlobalExceptionHandler),
        // not a plain-text body.
        mockMvc.perform(post("/api/alerts/{id}/acknowledge", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }
}
