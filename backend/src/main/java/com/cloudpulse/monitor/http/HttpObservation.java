package com.cloudpulse.monitor.http;

import java.net.URI;

/** Raw, bounded outcome of safely fetching one monitoring URL. */
public record HttpObservation(
        URI requestedUri,
        URI finalUri,
        int httpStatus,
        long responseTimeMs,
        String body,
        boolean bodyTruncated,
        int redirectCount,
        String failureMessage
) {
    public static HttpObservation success(
            URI requestedUri,
            URI finalUri,
            int httpStatus,
            long responseTimeMs,
            String body,
            boolean bodyTruncated,
            int redirectCount) {
        return new HttpObservation(
                requestedUri,
                finalUri,
                httpStatus,
                responseTimeMs,
                body,
                bodyTruncated,
                redirectCount,
                null
        );
    }

    public static HttpObservation failure(
            URI requestedUri,
            URI finalUri,
            long responseTimeMs,
            int redirectCount,
            String failureMessage) {
        return new HttpObservation(
                requestedUri,
                finalUri,
                0,
                responseTimeMs,
                "",
                false,
                redirectCount,
                failureMessage
        );
    }

    public boolean completed() {
        return failureMessage == null;
    }
}
