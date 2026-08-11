package com.cloudpulse.controller;

import java.time.Instant;
import java.util.UUID;

public record SslCertificateResponse(
        UUID serviceId,
        boolean applicable,
        boolean reachable,
        Instant expiresAt,
        Long daysRemaining,
        String subject,
        String message,
        SslCertificateStatus status) {

    public enum SslCertificateStatus { NOT_APPLICABLE, UNREACHABLE, VALID, EXPIRING_SOON, EXPIRED }
}
