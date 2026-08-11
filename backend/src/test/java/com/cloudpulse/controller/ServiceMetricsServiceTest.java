package com.cloudpulse.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceMetricsServiceTest {

    @Test
    void percentile95_usesNearestRankWithoutMutatingInput() {
        List<Long> responseTimes = List.of(100L, 1L, 20L, 50L, 10L);

        assertThat(ServiceMetricsService.percentile95(responseTimes)).isEqualTo(100L);
        assertThat(responseTimes).containsExactly(100L, 1L, 20L, 50L, 10L);
    }

    @Test
    void percentile95_emptySampleReturnsNull() {
        assertThat(ServiceMetricsService.percentile95(List.of())).isNull();
    }
}
