package com.cloudpulse.config;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registers the three demo services on first startup so the dashboard isn't empty.
 * Skipped once any service exists, so it never overwrites data an operator has edited.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final MonitoredServiceRepository serviceRepository;
    private final String standardUrl;
    private final String legacyUrl;
    private final String flakyUrl;

    public DemoDataSeeder(MonitoredServiceRepository serviceRepository,
                           @Value("${cloudpulse.demo.standard-url:http://localhost:8081/health}") String standardUrl,
                           @Value("${cloudpulse.demo.legacy-url:http://localhost:8082/health}") String legacyUrl,
                           @Value("${cloudpulse.demo.flaky-url:http://localhost:8083/health}") String flakyUrl) {
        this.serviceRepository = serviceRepository;
        this.standardUrl = standardUrl;
        this.legacyUrl = legacyUrl;
        this.flakyUrl = flakyUrl;
    }

    @Override
    public void run(String... args) {
        if (serviceRepository.count() > 0) {
            return;
        }

        serviceRepository.save(demoService(
                "Demo Standard", standardUrl, "Core Services", List.of("api", "standard")));
        serviceRepository.save(demoService(
                "Demo Legacy", legacyUrl, "Legacy Systems", List.of("api", "legacy")));
        serviceRepository.save(demoService(
                "Demo Flaky", flakyUrl, "Experimental", List.of("demo", "flaky")));
    }

    private MonitoredService demoService(String name, String url, String group, List<String> tags) {
        MonitoredService service = new MonitoredService(name, url, MonitorType.HTTP);
        service.setServiceGroup(group);
        service.setTags(tags);
        return service;
    }
}
