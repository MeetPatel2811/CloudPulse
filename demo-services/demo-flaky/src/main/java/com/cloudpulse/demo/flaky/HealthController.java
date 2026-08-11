package com.cloudpulse.demo.flaky;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() throws InterruptedException {
        int roll = ThreadLocalRandom.current().nextInt(3);
        Map<String, Object> body = new LinkedHashMap<>();

        if (roll == 0) {
            long delayMs = ThreadLocalRandom.current().nextLong(2000, 3000);
            Thread.sleep(delayMs);
            body.put("status", "UP");
            body.put("responseTimeMs", delayMs);
            return ResponseEntity.ok(body);
        }
        if (roll == 1) {
            body.put("status", "DOWN");
            body.put("responseTimeMs", 0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
        body.put("status", "UP");
        body.put("responseTimeMs", ThreadLocalRandom.current().nextInt(10, 60));
        return ResponseEntity.ok(body);
    }
}