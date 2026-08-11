package com.cloudpulse.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Previews a monitoring target without registering or persisting it. */
@RestController
@RequestMapping("/api/services/probe")
public class ProbeController {

    private final ProbeService probeService;

    public ProbeController(ProbeService probeService) {
        this.probeService = probeService;
    }

    @PostMapping
    public ProbeServiceResponse probe(@Valid @RequestBody ProbeServiceRequest request) {
        return probeService.probe(request.url());
    }
}
