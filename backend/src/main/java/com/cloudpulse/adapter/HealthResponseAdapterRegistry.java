package com.cloudpulse.adapter;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

/** Selects the first supporting adapter from Spring's explicitly ordered adapter list. */
@Component
public class HealthResponseAdapterRegistry {

    private final List<HealthResponseAdapter> adapters;

    public HealthResponseAdapterRegistry(List<HealthResponseAdapter> adapters) {
        List<HealthResponseAdapter> ordered = new ArrayList<>(adapters);
        AnnotationAwareOrderComparator.sort(ordered);
        this.adapters = List.copyOf(ordered);
    }

    public AdaptedHealthResponse adapt(String body, int httpStatus, long responseTimeMs) {
        HealthResponseAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(body))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No HTTP response adapter is configured"));

        return new AdaptedHealthResponse(
                adapter.adapt(body, httpStatus, responseTimeMs),
                adapter.format()
        );
    }
}
