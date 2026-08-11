package com.cloudpulse.demo.legacy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", "ok");
        body.put("latency", ThreadLocalRandom.current().nextInt(10, 60));
        return body;
    }
}
