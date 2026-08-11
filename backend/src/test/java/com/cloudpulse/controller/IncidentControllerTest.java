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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives {@link IncidentController} through MockMvc against the real Spring context
 * (H2 database, real repositories), matching the style of the other controller tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private AlertRepository alertRepository;

    private Alert seedAlert() {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8082/health", MonitorType.MOCK));
        return alertRepository.save(new Alert(service, Severity.CRITICAL, "Payment is DOWN"));
    }

    @Test
    void assign_thenGet_showsOwnerAndTimeline() throws Exception {
        Alert alert = seedAlert();

        mockMvc.perform(post("/api/incidents/{id}/assign", alert.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("alice"));

        mockMvc.perform(get("/api/incidents/{id}", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("alice"))
                .andExpect(jsonPath("$.activity[?(@.type == 'ASSIGNED')]").exists());
    }

    @Test
    void addNote_appearsOnTimeline() throws Exception {
        Alert alert = seedAlert();

        mockMvc.perform(post("/api/incidents/{id}/notes", alert.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Looking into it\",\"author\":\"meet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity[?(@.detail == 'Looking into it')]").exists());
    }

    @Test
    void assign_blankOwner_returns400() throws Exception {
        Alert alert = seedAlert();

        mockMvc.perform(post("/api/incidents/{id}/assign", alert.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownIncident_returns404() throws Exception {
        mockMvc.perform(get("/api/incidents/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
