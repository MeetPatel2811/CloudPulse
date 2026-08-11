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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class SloTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void updateTarget_savesValidAvailabilityObjective() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Checkout", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(put("/api/services/{id}/slo", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySloPercent\":99.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilitySloPercent").value(99.9));
    }

    @Test
    void updateTarget_rejectsOutOfRangeAndUnknownService() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Checkout", "http://localhost:8081/health", MonitorType.MOCK));

        mockMvc.perform(put("/api/services/{id}/slo", service.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySloPercent\":100}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/services/{id}/slo", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySloPercent\":99}"))
                .andExpect(status().isNotFound());
    }
}
