package com.cloudpulse.controller;

import com.cloudpulse.monitor.http.SslCertificateInspection;
import com.cloudpulse.monitor.http.SslCertificateInspector;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SslCertificateServiceTest {

    private final SslCertificateInspector inspector = mock(SslCertificateInspector.class);
    private final SslCertificateService service = new SslCertificateService(inspector);
    private final UUID serviceId = UUID.randomUUID();

    @Test
    void notApplicable_whenTargetIsNotHttps() {
        when(inspector.inspect("http://example.com")).thenReturn(SslCertificateInspection.notApplicable());

        SslCertificateResponse response = service.check(serviceId, "http://example.com");

        assertEquals(SslCertificateResponse.SslCertificateStatus.NOT_APPLICABLE, response.status());
    }

    @Test
    void unreachable_whenHandshakeFails() {
        when(inspector.inspect("https://example.com")).thenReturn(SslCertificateInspection.unreachable("boom"));

        SslCertificateResponse response = service.check(serviceId, "https://example.com");

        assertEquals(SslCertificateResponse.SslCertificateStatus.UNREACHABLE, response.status());
    }

    @Test
    void expired_whenNotAfterIsInThePast() {
        Instant expiry = Instant.now().minus(1, ChronoUnit.DAYS);
        when(inspector.inspect("https://example.com")).thenReturn(SslCertificateInspection.of(expiry, "CN=example.com"));

        SslCertificateResponse response = service.check(serviceId, "https://example.com");

        assertEquals(SslCertificateResponse.SslCertificateStatus.EXPIRED, response.status());
    }

    @Test
    void expiringSoon_whenWithinThreshold() {
        Instant expiry = Instant.now().plus(5, ChronoUnit.DAYS);
        when(inspector.inspect("https://example.com")).thenReturn(SslCertificateInspection.of(expiry, "CN=example.com"));

        SslCertificateResponse response = service.check(serviceId, "https://example.com");

        assertEquals(SslCertificateResponse.SslCertificateStatus.EXPIRING_SOON, response.status());
    }

    @Test
    void valid_whenFarFromExpiry() {
        Instant expiry = Instant.now().plus(90, ChronoUnit.DAYS);
        when(inspector.inspect("https://example.com")).thenReturn(SslCertificateInspection.of(expiry, "CN=example.com"));

        SslCertificateResponse response = service.check(serviceId, "https://example.com");

        assertEquals(SslCertificateResponse.SslCertificateStatus.VALID, response.status());
        assertTrue(response.daysRemaining() >= 89);
    }
}
