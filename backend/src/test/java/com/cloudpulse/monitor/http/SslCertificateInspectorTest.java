package com.cloudpulse.monitor.http;

import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SslCertificateInspectorTest {

    private SafeUrlValidator validator() {
        HttpMonitoringProperties properties = new HttpMonitoringProperties();
        properties.setAllowedHosts(List.of("secure.example"));
        return new SafeUrlValidator(properties);
    }

    @Test
    void httpUrl_isNotApplicable() {
        SslCertificateInspector inspector = new SslCertificateInspector(
                validator(), (host, port) -> mock(X509Certificate.class));

        SslCertificateInspection result = inspector.inspect("http://secure.example/health");

        assertFalse(result.applicable());
        assertFalse(result.reachable());
    }

    @Test
    void privateAddress_isUnreachableWithValidatorMessage() {
        SslCertificateInspector inspector = new SslCertificateInspector(
                validator(), (host, port) -> mock(X509Certificate.class));

        SslCertificateInspection result = inspector.inspect("https://169.254.169.254/latest/meta-data");

        assertTrue(result.applicable());
        assertFalse(result.reachable());
        assertEquals("URL resolves to a private or restricted address", result.message());
    }

    @Test
    void httpsUrl_successfulHandshake_returnsCertificateExpiry() {
        Instant expiry = Instant.parse("2027-01-01T00:00:00Z");
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getNotAfter()).thenReturn(Date.from(expiry));
        when(certificate.getSubjectX500Principal())
                .thenReturn(new javax.security.auth.x500.X500Principal("CN=secure.example"));

        SslCertificateInspector inspector = new SslCertificateInspector(
                validator(), (host, port) -> certificate);

        SslCertificateInspection result = inspector.inspect("https://secure.example/health");

        assertTrue(result.applicable());
        assertTrue(result.reachable());
        assertEquals(expiry, result.expiresAt());
        assertEquals("CN=secure.example", result.subject());
    }

    @Test
    void httpsUrl_handshakeFailure_isUnreachable() {
        SslCertificateInspector inspector = new SslCertificateInspector(validator(), (host, port) -> {
            throw new java.io.IOException("handshake failed");
        });

        SslCertificateInspection result = inspector.inspect("https://secure.example/health");

        assertTrue(result.applicable());
        assertFalse(result.reachable());
        assertTrue(result.message().contains("IOException"));
    }
}
