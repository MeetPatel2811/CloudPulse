package com.cloudpulse.monitor.http;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.cert.X509Certificate;

/** Safely inspects a monitored URL's TLS certificate, reusing the same SSRF guard as HTTP checks. */
@Component
public class SslCertificateInspector {

    private final SafeUrlValidator urlValidator;
    private final TlsCertificateFetcher certificateFetcher;

    public SslCertificateInspector(SafeUrlValidator urlValidator, TlsCertificateFetcher certificateFetcher) {
        this.urlValidator = urlValidator;
        this.certificateFetcher = certificateFetcher;
    }

    public SslCertificateInspection inspect(String rawUrl) {
        URI uri;
        try {
            uri = urlValidator.validate(rawUrl);
        } catch (IllegalArgumentException exception) {
            return SslCertificateInspection.unreachable(exception.getMessage());
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return SslCertificateInspection.notApplicable();
        }

        int port = uri.getPort() == -1 ? 443 : uri.getPort();
        try {
            X509Certificate certificate = certificateFetcher.fetchLeafCertificate(uri.getHost(), port);
            return SslCertificateInspection.of(
                    certificate.getNotAfter().toInstant(),
                    certificate.getSubjectX500Principal().getName());
        } catch (Exception exception) {
            return SslCertificateInspection.unreachable(
                    "TLS handshake failed: " + exception.getClass().getSimpleName());
        }
    }
}
