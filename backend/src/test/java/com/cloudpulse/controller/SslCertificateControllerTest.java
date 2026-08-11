package com.cloudpulse.controller;

import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.monitor.http.TlsCertificateFetcher;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cloudpulse.scheduler.enabled=false",
        "cloudpulse.http.allowed-hosts=secure.example"
})
@AutoConfigureMockMvc
@Import(SslCertificateControllerTest.FakeCertificateFetcherConfiguration.class)
class SslCertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Test
    void check_httpsService_returnsExpiryAndStatus() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Secure API", "https://secure.example/health", MonitorType.HTTP));

        mockMvc.perform(get("/api/services/{id}/ssl-certificate", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicable").value(true))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.subject").value("CN=secure.example"));
    }

    @Test
    void check_httpService_isNotApplicable() throws Exception {
        MonitoredService service = serviceRepository.save(
                new MonitoredService("Plain API", "http://secure.example/health", MonitorType.HTTP));

        mockMvc.perform(get("/api/services/{id}/ssl-certificate", service.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicable").value(false))
                .andExpect(jsonPath("$.status").value("NOT_APPLICABLE"));
    }

    @Test
    void check_unknownService_returns404() throws Exception {
        mockMvc.perform(get("/api/services/{id}/ssl-certificate", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class FakeCertificateFetcherConfiguration {

        @Bean
        @Primary
        TlsCertificateFetcher fakeTlsCertificateFetcher() {
            return (host, port) -> {
                X509Certificate certificate = mock(X509Certificate.class);
                when(certificate.getNotAfter()).thenReturn(Date.from(Instant.now().plus(60, java.time.temporal.ChronoUnit.DAYS)));
                when(certificate.getSubjectX500Principal())
                        .thenReturn(new javax.security.auth.x500.X500Principal("CN=" + host));
                return certificate;
            };
        }
    }
}
