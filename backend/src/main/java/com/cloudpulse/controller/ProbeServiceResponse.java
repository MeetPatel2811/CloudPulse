package com.cloudpulse.controller;

import com.cloudpulse.adapter.HttpResponseFormat;

public record ProbeServiceResponse(
        boolean reachable,
        int httpStatus,
        long responseTimeMs,
        HttpResponseFormat detectedMode,
        String finalUrl,
        int redirectCount,
        boolean responseBodyTruncated,
        String message
) {
}
