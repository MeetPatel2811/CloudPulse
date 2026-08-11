package com.cloudpulse.controller;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MaintenanceWindowRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class MaintenanceWindowControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private MaintenanceWindowRepository windowRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MonitoredService saveService(String name) {
        return serviceRepository.save(new MonitoredService(name, "http://localhost:8081/health", MonitorType.MOCK));
    }

    @Test
    void schedule_thenListIncludesIt() throws Exception {
        MonitoredService service = saveService("Payment");
        String body = objectMapper.writeValueAsString(Map.of(
                "startsAt", "2026-09-01T02:00:00Z",
                "endsAt", "2026-09-01T04:00:00Z",
                "reason", "Database migration"));

        mockMvc.perform(post("/api/services/{id}/maintenance-windows", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(service.getId().toString()))
                .andExpect(jsonPath("$.reason").value("Database migration"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/services/{id}/maintenance-windows", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("Database migration"));
    }

    @Test
    void schedule_endsBeforeStarts_returns400() throws Exception {
        MonitoredService service = saveService("Payment");
        String body = objectMapper.writeValueAsString(Map.of(
                "startsAt", "2026-09-01T04:00:00Z",
                "endsAt", "2026-09-01T02:00:00Z"));

        mockMvc.perform(post("/api/services/{id}/maintenance-windows", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endsAt must be after startsAt"));
    }

    @Test
    void schedule_overlappingWindow_returns409() throws Exception {
        MonitoredService service = saveService("Payment");
        windowRepository.save(new com.cloudpulse.model.MaintenanceWindow(
                service, Instant.parse("2026-09-01T02:00:00Z"), Instant.parse("2026-09-01T04:00:00Z"), null));

        String body = objectMapper.writeValueAsString(Map.of(
                "startsAt", "2026-09-01T03:00:00Z",
                "endsAt", "2026-09-01T05:00:00Z"));

        mockMvc.perform(post("/api/services/{id}/maintenance-windows", service.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void schedule_unknownService_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "startsAt", "2026-09-01T02:00:00Z",
                "endsAt", "2026-09-01T04:00:00Z"));

        mockMvc.perform(post("/api/services/{id}/maintenance-windows", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_removesWindow() throws Exception {
        MonitoredService service = saveService("Payment");
        var window = windowRepository.save(new com.cloudpulse.model.MaintenanceWindow(
                service, Instant.parse("2026-09-01T02:00:00Z"), Instant.parse("2026-09-01T04:00:00Z"), null));

        mockMvc.perform(delete("/api/services/{id}/maintenance-windows/{windowId}",
                        service.getId(), window.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/services/{id}/maintenance-windows", service.getId()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancel_unknownWindow_returns404() throws Exception {
        MonitoredService service = saveService("Payment");

        mockMvc.perform(delete("/api/services/{id}/maintenance-windows/{windowId}",
                        service.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
