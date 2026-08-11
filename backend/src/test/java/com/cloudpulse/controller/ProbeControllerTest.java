package com.cloudpulse.controller;

import com.cloudpulse.monitor.http.HttpTransport;
import com.cloudpulse.monitor.http.HttpTransportResponse;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cloudpulse.scheduler.enabled=false",
        "cloudpulse.http.allowed-hosts=probe.example"
})
@AutoConfigureMockMvc
@Import(ProbeControllerTest.FakeTransportConfiguration.class)
class ProbeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void probeGenericWebsite_returnsPreviewWithoutPersisting() throws Exception {
        long countBefore = serviceRepository.count();

        mockMvc.perform(post("/api/services/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://probe.example/home\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.detectedMode").value("GENERIC_HTTP"))
                .andExpect(jsonPath("$.finalUrl").value("https://probe.example/home"))
                .andExpect(jsonPath("$.message").value("Connection successful"));

        assertEquals(countBefore, serviceRepository.count());
    }

    @Test
    void probeStructuredHealthEndpoint_detectsStandardJson() throws Exception {
        mockMvc.perform(post("/api/services/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://probe.example/standard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.detectedMode").value("STANDARD_JSON"))
                .andExpect(jsonPath("$.responseTimeMs").value(42));
    }

    @Test
    void probePrivateAddress_returnsConsistent400Error() throws Exception {
        mockMvc.perform(post("/api/services/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://169.254.169.254/latest/meta-data\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("URL resolves to a private or restricted address"));
    }

    @Test
    void probeBlankUrl_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/services/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @TestConfiguration
    static class FakeTransportConfiguration {

        @Bean
        @Primary
        HttpTransport fakeHttpTransport() {
            return uri -> {
                if (uri.getPath().equals("/standard")) {
                    return new HttpTransportResponse(
                            200,
                            "{\"status\":\"UP\",\"responseTimeMs\":42}",
                            false,
                            null
                    );
                }
                return new HttpTransportResponse(200, "<html>Website</html>", false, null);
            };
        }
    }
}
