package com.cloudpulse.controller;

import com.cloudpulse.model.EvaluationPolicy;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class EvaluationPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void updatePolicy_persistsStrictStrategy() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.HTTP));

        mockMvc.perform(patch("/api/services/{id}/evaluation-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationPolicy\":\"STRICT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(service.getId().toString()))
                .andExpect(jsonPath("$.serviceName").value("Payment"))
                .andExpect(jsonPath("$.evaluationPolicy").value("STRICT"))
                .andExpect(jsonPath("$.currentStatus").value("UNKNOWN"));

        MonitoredService updated = serviceRepository.findById(service.getId()).orElseThrow();
        assertEquals(EvaluationPolicy.STRICT, updated.getActiveEvaluationStrategy());
    }

    @Test
    void updatePolicy_missingPolicy_returns400ErrorResponse() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("User", "http://localhost:8082/health", MonitorType.HTTP));

        mockMvc.perform(patch("/api/services/{id}/evaluation-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updatePolicy_unknownService_returns404ErrorResponse() throws Exception {
        mockMvc.perform(patch("/api/services/{id}/evaluation-policy", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationPolicy\":\"NORMAL\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void updatePolicy_invalidEnum_returnsConsistent400ErrorResponse() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Invalid policy", "http://localhost:8081/health", MonitorType.HTTP));

        mockMvc.perform(patch("/api/services/{id}/evaluation-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationPolicy\":\"AGGRESSIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updatePolicy_malformedServiceId_returnsConsistent400ErrorResponse() throws Exception {
        mockMvc.perform(patch("/api/services/{id}/evaluation-policy", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationPolicy\":\"NORMAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }
}
