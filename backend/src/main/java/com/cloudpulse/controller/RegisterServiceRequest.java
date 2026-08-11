package com.cloudpulse.controller;

import com.cloudpulse.model.MonitorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * checkIntervalSeconds is optional: omit it to keep the default (create) or leave the
 * existing value unchanged (update). When present it must be one of
 * {@link ServiceController#ALLOWED_CHECK_INTERVALS_SECONDS}.
 * expectedKeyword is required when monitorType is KEYWORD and ignored otherwise.
 * serviceGroup and tags are always optional, free-form organizational metadata.
 */
public record RegisterServiceRequest(
        @NotBlank String name,
        @NotBlank String healthUrl,
        @NotNull MonitorType monitorType,
        Long checkIntervalSeconds,
        String expectedKeyword,
        @Size(max = 80) String serviceGroup,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> tags) {

    public RegisterServiceRequest(String name, String healthUrl, MonitorType monitorType) {
        this(name, healthUrl, monitorType, null, null, null, List.of());
    }

    public RegisterServiceRequest(String name, String healthUrl, MonitorType monitorType, Long checkIntervalSeconds) {
        this(name, healthUrl, monitorType, checkIntervalSeconds, null, null, List.of());
    }

    /** Convenience for KEYWORD-monitor registration/tests. */
    public RegisterServiceRequest(
            String name, String healthUrl, MonitorType monitorType,
            Long checkIntervalSeconds, String expectedKeyword) {
        this(name, healthUrl, monitorType, checkIntervalSeconds, expectedKeyword, null, List.of());
    }

    /** Convenience for service-group/tag registration/tests. */
    public RegisterServiceRequest(
            String name, String healthUrl, MonitorType monitorType,
            String serviceGroup, List<String> tags) {
        this(name, healthUrl, monitorType, null, null, serviceGroup, tags);
    }
}
