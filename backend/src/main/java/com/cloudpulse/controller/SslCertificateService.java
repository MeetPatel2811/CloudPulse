package com.cloudpulse.controller;

import com.cloudpulse.monitor.http.SslCertificateInspection;
import com.cloudpulse.monitor.http.SslCertificateInspector;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Turns a raw {@link SslCertificateInspection} into an API response with a computed status. */
@Service
public class SslCertificateService {

    private static final long EXPIRING_SOON_THRESHOLD_DAYS = 14;

    private final SslCertificateInspector inspector;

    public SslCertificateService(SslCertificateInspector inspector) {
        this.inspector = inspector;
    }

    public SslCertificateResponse check(UUID serviceId, String healthUrl) {
        SslCertificateInspection inspection = inspector.inspect(healthUrl);

        Long daysRemaining = inspection.expiresAt() != null
                ? ChronoUnit.DAYS.between(Instant.now(), inspection.expiresAt())
                : null;

        return new SslCertificateResponse(
                serviceId,
                inspection.applicable(),
                inspection.reachable(),
                inspection.expiresAt(),
                daysRemaining,
                inspection.subject(),
                inspection.message(),
                statusFor(inspection, daysRemaining));
    }

    private SslCertificateResponse.SslCertificateStatus statusFor(SslCertificateInspection inspection, Long daysRemaining) {
        if (!inspection.applicable()) {
            return SslCertificateResponse.SslCertificateStatus.NOT_APPLICABLE;
        }
        if (!inspection.reachable() || daysRemaining == null) {
            return SslCertificateResponse.SslCertificateStatus.UNREACHABLE;
        }
        if (daysRemaining < 0) {
            return SslCertificateResponse.SslCertificateStatus.EXPIRED;
        }
        if (daysRemaining <= EXPIRING_SOON_THRESHOLD_DAYS) {
            return SslCertificateResponse.SslCertificateStatus.EXPIRING_SOON;
        }
        return SslCertificateResponse.SslCertificateStatus.VALID;
    }
}
