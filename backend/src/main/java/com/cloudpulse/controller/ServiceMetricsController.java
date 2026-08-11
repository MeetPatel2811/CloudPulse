package com.cloudpulse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only operational metrics for one monitored service. */
@RestController
@RequestMapping("/api/services/{serviceId}/metrics")
public class ServiceMetricsController {

    private final ServiceMetricsService metricsService;

    public ServiceMetricsController(ServiceMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ServiceMetricsResponse getMetrics(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "24h") String window) {
        return metricsService.getMetrics(serviceId, MetricsWindow.parse(window));
    }
}
