package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ServiceMetricsService {

    static final int MAX_P95_SAMPLES = 10_000;

    private final MonitoredServiceRepository serviceRepository;
    private final HealthCheckResultRepository resultRepository;

    public ServiceMetricsService(
            MonitoredServiceRepository serviceRepository,
            HealthCheckResultRepository resultRepository) {
        this.serviceRepository = serviceRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public ServiceMetricsResponse getMetrics(UUID serviceId, MetricsWindow window) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));

        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(window.duration());
        long totalChecks = resultRepository.countByService_IdAndCheckedAtBetween(
                serviceId, windowStart, windowEnd);
        long successfulChecks = resultRepository
                .countByService_IdAndReachableTrueAndCheckedAtBetween(
                        serviceId, windowStart, windowEnd);
        Double averageResponseTimeMs = resultRepository.averageSuccessfulResponseTimeMs(
                serviceId, windowStart, windowEnd);

        List<HealthCheckResult> p95Results = resultRepository
                .findByService_IdAndReachableTrueAndCheckedAtBetweenOrderByCheckedAtDesc(
                        serviceId,
                        windowStart,
                        windowEnd,
                        PageRequest.of(0, MAX_P95_SAMPLES));
        List<Long> responseTimes = p95Results.stream()
                .map(HealthCheckResult::getResponseTimeMs)
                .toList();

        double availabilityPercent = percentage(successfulChecks, totalChecks);
        double sloTarget = service.getAvailabilitySloPercent();

        return new ServiceMetricsResponse(
                service.getId(),
                service.getName(),
                window.value(),
                windowStart,
                windowEnd,
                service.getCurrentStatus(),
                service.isEnabled(),
                service.getActiveEvaluationStrategy(),
                totalChecks,
                successfulChecks,
                totalChecks - successfulChecks,
                availabilityPercent,
                sloTarget,
                errorBudgetRemaining(totalChecks - successfulChecks, totalChecks, sloTarget),
                totalChecks == 0 || availabilityPercent >= sloTarget,
                roundToOneDecimal(averageResponseTimeMs),
                percentile95(responseTimes),
                responseTimes.size(),
                successfulChecks > responseTimes.size());
    }

    static Long percentile95(List<Long> responseTimes) {
        if (responseTimes.isEmpty()) {
            return null;
        }

        List<Long> sorted = new ArrayList<>(responseTimes);
        Collections.sort(sorted);
        int nearestRankIndex = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, nearestRankIndex));
    }

    private double percentage(long successfulChecks, long totalChecks) {
        if (totalChecks == 0) {
            return 0.0;
        }
        return Math.round((successfulChecks * 10000.0) / totalChecks) / 100.0;
    }

    private double errorBudgetRemaining(long failedChecks, long totalChecks, double sloTarget) {
        if (totalChecks == 0) {
            return 100.0;
        }
        double allowedErrorPercent = 100.0 - sloTarget;
        double observedErrorPercent = failedChecks * 100.0 / totalChecks;
        double consumedPercent = observedErrorPercent / allowedErrorPercent * 100.0;
        return Math.round(Math.max(0.0, 100.0 - consumedPercent) * 100.0) / 100.0;
    }

    private Double roundToOneDecimal(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }
}
