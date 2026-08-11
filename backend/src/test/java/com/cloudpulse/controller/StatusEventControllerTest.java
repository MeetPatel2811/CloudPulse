package com.cloudpulse.controller;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots the real Spring context and drives StatusEventController through MockMvc,
// matching the style of AlertControllerTest.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class StatusEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private StatusEventRepository statusEventRepository;

    @Test
    void listForService_returnsRecordedEventsNewestFirst() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK));
        statusEventRepository.save(new StatusEvent(
                service, ServiceStatus.HEALTHY, ServiceStatus.DEGRADED, "slowed down"));
        Thread.sleep(10); // guarantee the second event has a strictly later occurredAt
        statusEventRepository.save(new StatusEvent(
                service, ServiceStatus.DEGRADED, ServiceStatus.DOWN, "went down"));

        mockMvc.perform(get("/api/services/{id}/status-events", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].newStatus").value("DOWN"))       // newest first
                .andExpect(jsonPath("$[1].newStatus").value("DEGRADED"))
                .andExpect(jsonPath("$[0].serviceName").value("Payment"));
    }

    @Test
    void listForService_unknownService_returns404() throws Exception {
        mockMvc.perform(get("/api/services/{id}/status-events", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
