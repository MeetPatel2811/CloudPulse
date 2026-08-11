package com.cloudpulse.controller;

import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.repository.MonitoredServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

/** On-demand, non-persisting TLS certificate check for a registered service. */
@RestController
@RequestMapping("/api/services/{serviceId}/ssl-certificate")
public class SslCertificateController {

    private final MonitoredServiceRepository serviceRepository;
    private final SslCertificateService sslCertificateService;

    public SslCertificateController(
            MonitoredServiceRepository serviceRepository,
            SslCertificateService sslCertificateService) {
        this.serviceRepository = serviceRepository;
        this.sslCertificateService = sslCertificateService;
    }

    @GetMapping
    public SslCertificateResponse check(@PathVariable UUID serviceId) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service not found: " + serviceId));
        return sslCertificateService.check(serviceId, service.getHealthUrl());
    }
}
