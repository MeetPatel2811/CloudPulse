package com.cloudpulse.monitor.http;

/** A bounded transport response. Headers are intentionally not exposed beyond Location. */
public record HttpTransportResponse(
        int statusCode,
        String body,
        boolean bodyTruncated,
        String redirectLocation
) {
}
