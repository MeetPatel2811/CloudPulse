package com.cloudpulse.controller;

import java.time.Duration;
import java.util.Arrays;

/** Supported dashboard aggregation windows. */
public enum MetricsWindow {
    ONE_HOUR("1h", Duration.ofHours(1)),
    SIX_HOURS("6h", Duration.ofHours(6)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24)),
    SEVEN_DAYS("7d", Duration.ofDays(7));

    private final String value;
    private final Duration duration;

    MetricsWindow(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    public String value() {
        return value;
    }

    public Duration duration() {
        return duration;
    }

    public static MetricsWindow parse(String value) {
        return Arrays.stream(values())
                .filter(window -> window.value.equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "window must be one of: 1h, 6h, 24h, 7d"));
    }
}
