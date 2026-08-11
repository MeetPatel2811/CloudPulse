package com.cloudpulse.controller;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class PublicStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void publicStatus_returnsEnabledServicesWithoutInternalConfiguration() throws Exception {
        MonitoredService service = new MonitoredService(
                "Payments API", "https://private.example.com/health", MonitorType.MOCK);
        service.setServiceGroup("Revenue");
        service.setTags(List.of("critical", "internal"));
        service.setCurrentStatus(ServiceStatus.DOWN);
        serviceRepository.save(service);

        MonitoredService disabled = new MonitoredService(
                "Paused service", "https://paused.example.com/health", MonitorType.MOCK);
        disabled.setEnabled(false);
        serviceRepository.save(disabled);

        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.overallStatus").value("MAJOR_OUTAGE"))
                .andExpect(jsonPath("$.services[*].name", hasItem("Payments API")))
                .andExpect(jsonPath("$.services[*].name", not(hasItem("Paused service"))))
                .andExpect(jsonPath("$.services[?(@.name == 'Payments API')].serviceGroup", hasItem("Revenue")))
                .andExpect(jsonPath("$.services[?(@.name == 'Payments API')].status", hasItem("DOWN")))
                .andExpect(jsonPath("$.services[0].healthUrl").doesNotExist())
                .andExpect(jsonPath("$.services[0].tags").doesNotExist());
    }
}
