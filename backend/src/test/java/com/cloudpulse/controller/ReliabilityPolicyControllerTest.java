package com.cloudpulse.controller;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class ReliabilityPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void getPolicy_reportsTheNormalStrategyDefault() throws Exception {
        MonitoredService service = saveService("Payments");

        mockMvc.perform(get("/api/services/{id}/reliability-policy", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latencyThresholdMs").doesNotExist())
                .andExpect(jsonPath("$.effectiveLatencyThresholdMs").value(1_000))
                .andExpect(jsonPath("$.alertFailureThreshold").value(1))
                .andExpect(jsonPath("$.consecutiveFailureCount").value(0));
    }

    @Test
    void updatePolicy_persistsAndReportsACustomThreshold() throws Exception {
        MonitoredService service = saveService("Checkout");

        mockMvc.perform(patch("/api/services/{id}/reliability-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latencyThresholdMs\":750}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(service.getId().toString()))
                .andExpect(jsonPath("$.latencyThresholdMs").value(750))
                .andExpect(jsonPath("$.effectiveLatencyThresholdMs").value(750));

        assertEquals(750L, serviceRepository.findById(service.getId()).orElseThrow().getLatencyThresholdMs());
    }

    @Test
    void updatePolicy_nullRestoresTheStrategyDefault() throws Exception {
        MonitoredService service = saveService("Catalog");
        service.setLatencyThresholdMs(800L);
        serviceRepository.save(service);

        mockMvc.perform(patch("/api/services/{id}/reliability-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latencyThresholdMs\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latencyThresholdMs").doesNotExist())
                .andExpect(jsonPath("$.effectiveLatencyThresholdMs").value(1_000));

        assertNull(serviceRepository.findById(service.getId()).orElseThrow().getLatencyThresholdMs());
    }

    @Test
    void updatePolicy_rejectsThresholdOutsideTheSupportedRange() throws Exception {
        MonitoredService service = saveService("Search");

        mockMvc.perform(patch("/api/services/{id}/reliability-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latencyThresholdMs\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updatePolicy_persistsTheAlertFailureThreshold() throws Exception {
        MonitoredService service = saveService("Notifications");
        service.recordUnhealthyCheck();
        serviceRepository.save(service);

        mockMvc.perform(patch("/api/services/{id}/reliability-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latencyThresholdMs\":null,\"alertFailureThreshold\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertFailureThreshold").value(3))
                .andExpect(jsonPath("$.consecutiveFailureCount").value(0));

        assertEquals(3, serviceRepository.findById(service.getId()).orElseThrow().getAlertFailureThreshold());
    }

    @Test
    void updatePolicy_rejectsAnInvalidAlertFailureThreshold() throws Exception {
        MonitoredService service = saveService("Identity");

        mockMvc.perform(patch("/api/services/{id}/reliability-policy", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latencyThresholdMs\":null,\"alertFailureThreshold\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getPolicy_unknownServiceReturns404() throws Exception {
        mockMvc.perform(get("/api/services/{id}/reliability-policy", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private MonitoredService saveService(String name) {
        return serviceRepository.save(
                new MonitoredService(name, "http://localhost:8081/health", MonitorType.HTTP));
    }
}
