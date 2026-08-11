package com.cloudpulse.controller;

import com.cloudpulse.command.DisableMonitoringCommand;
import com.cloudpulse.command.EnableMonitoringCommand;
import com.cloudpulse.command.RunHealthCheckCommand;
import com.cloudpulse.handler.HealthCheckProcessor;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.monitor.HttpMonitorCreator;
import com.cloudpulse.monitor.KeywordMonitorCreator;
import com.cloudpulse.monitor.MockMonitorCreator;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.monitor.http.SafeUrlValidator;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    /** The only check intervals selectable from the UI: 15s, 30s, 1m, 5m. */
    static final Set<Long> ALLOWED_CHECK_INTERVALS_SECONDS = Set.of(15L, 30L, 60L, 300L);

    private final MonitoredServiceRepository serviceRepository;
    private final HealthCheckResultRepository resultRepository;
    private final StatusEventRepository statusEventRepository;
    private final AlertRepository alertRepository;
    private final HealthCheckProcessor processor;
    private final HttpMonitorCreator httpMonitorCreator;
    private final MockMonitorCreator mockMonitorCreator;
    private final KeywordMonitorCreator keywordMonitorCreator;
    private final SafeUrlValidator safeUrlValidator;

    public ServiceController(MonitoredServiceRepository serviceRepository,
                             HealthCheckResultRepository resultRepository,
                             StatusEventRepository statusEventRepository,
                             AlertRepository alertRepository,
                             HealthCheckProcessor processor,
                             HttpMonitorCreator httpMonitorCreator,
                             MockMonitorCreator mockMonitorCreator,
                             KeywordMonitorCreator keywordMonitorCreator,
                             SafeUrlValidator safeUrlValidator) {
        this.serviceRepository = serviceRepository;
        this.resultRepository = resultRepository;
        this.statusEventRepository = statusEventRepository;
        this.alertRepository = alertRepository;
        this.processor = processor;
        this.httpMonitorCreator = httpMonitorCreator;
        this.mockMonitorCreator = mockMonitorCreator;
        this.keywordMonitorCreator = keywordMonitorCreator;
        this.safeUrlValidator = safeUrlValidator;
    }

    @GetMapping
    public List<MonitoredService> listServices() {
        return serviceRepository.findAll();
    }

    @GetMapping("/{id}")
    public MonitoredService getService(@PathVariable UUID id) {
        return findServiceOrThrow(id);
    }

    @PostMapping
    public MonitoredService registerService(@Valid @RequestBody RegisterServiceRequest request) {
        validateHttpTarget(request);
        MonitoredService service =
                new MonitoredService(request.name(), request.healthUrl(), request.monitorType());
        if (request.checkIntervalSeconds() != null) {
            service.setCheckIntervalSeconds(validatedInterval(request.checkIntervalSeconds()));
        }
        service.setExpectedKeyword(validatedKeyword(request));
        applyMetadata(service, request);
        return serviceRepository.save(service);
    }

    /** Updates an existing service's editable fields (name, health URL, monitor type, check interval, keyword, metadata). */
    @PutMapping("/{id}")
    public MonitoredService updateService(@PathVariable UUID id,
                                          @Valid @RequestBody RegisterServiceRequest request) {
        MonitoredService service = findServiceOrThrow(id);
        validateHttpTarget(request);
        service.setName(request.name());
        service.setHealthUrl(request.healthUrl());
        service.setMonitorType(request.monitorType());
        if (request.checkIntervalSeconds() != null) {
            service.setCheckIntervalSeconds(validatedInterval(request.checkIntervalSeconds()));
        }
        service.setExpectedKeyword(validatedKeyword(request));
        applyMetadata(service, request);
        return serviceRepository.save(service);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable UUID id) {
        MonitoredService service = findServiceOrThrow(id);
        if (hasHistory(id)) {
            throw new ServiceHasHistoryException(
                    "Cannot delete a service with monitoring history. Disable it instead.");
        }
        serviceRepository.delete(service);
    }

    /** True if the service still has any child records (health-check results, status events, or alerts). */
    private boolean hasHistory(UUID id) {
        return !resultRepository.findByService_IdOrderByCheckedAtDesc(id).isEmpty()
                || !statusEventRepository.findByService_IdOrderByOccurredAtDesc(id).isEmpty()
                || !alertRepository.findByService_IdOrderByCreatedAtDesc(id).isEmpty();
    }

    private void validateHttpTarget(RegisterServiceRequest request) {
        if (request.monitorType() == MonitorType.HTTP || request.monitorType() == MonitorType.KEYWORD) {
            safeUrlValidator.validate(request.healthUrl());
        }
    }

    /** KEYWORD requires non-blank text to look for; any other type ignores/clears it. */
    private String validatedKeyword(RegisterServiceRequest request) {
        if (request.monitorType() != MonitorType.KEYWORD) {
            return null;
        }
        if (request.expectedKeyword() == null || request.expectedKeyword().isBlank()) {
            throw new IllegalArgumentException("expectedKeyword is required for the KEYWORD monitor type");
        }
        return request.expectedKeyword();
    }

    /** Rejects any interval outside the fixed set the UI offers (15s / 30s / 1m / 5m). */
    private long validatedInterval(long seconds) {
        if (!ALLOWED_CHECK_INTERVALS_SECONDS.contains(seconds)) {
            throw new IllegalArgumentException(
                    "checkIntervalSeconds must be one of " + ALLOWED_CHECK_INTERVALS_SECONDS);
        }
        return seconds;
    }

    private void applyMetadata(MonitoredService service, RegisterServiceRequest request) {
        String group = request.serviceGroup();
        service.setServiceGroup(group == null || group.isBlank() ? null : group.trim());

        Set<String> normalizedTags = new LinkedHashSet<>();
        if (request.tags() != null) {
            request.tags().stream()
                    .map(String::trim)
                    .map(tag -> tag.toLowerCase(Locale.ROOT))
                    .forEach(normalizedTags::add);
        }
        service.setTags(normalizedTags);
    }

    /** Runs an immediate manual health check via {@link RunHealthCheckCommand} and returns the updated service. */
    @PostMapping("/{id}/check")
    public MonitoredService runCheck(@PathVariable UUID id) {
        MonitoredService service = findServiceOrThrow(id);
        ServiceMonitor monitor = resolveMonitor(service);
        new RunHealthCheckCommand(service, monitor, processor, resultRepository, serviceRepository).execute();
        return service;
    }

    @PostMapping("/{id}/enable")
    public MonitoredService enable(@PathVariable UUID id) {
        new EnableMonitoringCommand(id, serviceRepository).execute();
        return findServiceOrThrow(id);
    }

    @PostMapping("/{id}/disable")
    public MonitoredService disable(@PathVariable UUID id) {
        new DisableMonitoringCommand(id, serviceRepository).execute();
        return findServiceOrThrow(id);
    }

    private MonitoredService findServiceOrThrow(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + id));
    }

    /** Factory Method — pick the {@link ServiceMonitor} that matches this service's monitor type. */
    private ServiceMonitor resolveMonitor(MonitoredService service) {
        return switch (service.getMonitorType()) {
            case HTTP -> httpMonitorCreator.createMonitor();
            case MOCK -> mockMonitorCreator.createMonitor();
            case KEYWORD -> keywordMonitorCreator.createMonitor();
        };
    }
}
