package com.cloudpulse.monitor.http;

import java.time.Instant;

/** Result of inspecting a target's TLS certificate. Only {@code https} URLs are applicable. */
public record SslCertificateInspection(
        boolean applicable,
        boolean reachable,
        Instant expiresAt,
        String subject,
        String message) {

    public static SslCertificateInspection notApplicable() {
        return new SslCertificateInspection(false, false, null, null, "Not an HTTPS target");
    }

    public static SslCertificateInspection unreachable(String message) {
        return new SslCertificateInspection(true, false, null, null, message);
    }

    public static SslCertificateInspection of(Instant expiresAt, String subject) {
        return new SslCertificateInspection(true, true, expiresAt, subject, "Certificate retrieved");
    }
}
