package com.cloudpulse.integration;

import com.cloudpulse.command.AcknowledgeAlertCommand;
import com.cloudpulse.command.ResolveAlertCommand;
import com.cloudpulse.command.RunHealthCheckCommand;
import com.cloudpulse.handler.HealthCheckProcessor;
import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.monitor.ServiceMonitor;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.HealthCheckResultRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// End-to-end test for the processing and alert workflow.
// Unlike the unit tests, this boots the real Spring context (H2 database, real
// repositories, real HealthCheckProcessor chain) and runs a RunHealthCheckCommand
// through a full lifecycle:
// HEALTHY -> DEGRADED -> DOWN -> RECOVERED -> HEALTHY, then acknowledges and
// resolves an alert. Run it to watch the chain work against a real database.
@SpringBootTest
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class ProcessingWorkflowIntegrationTest {

    @Autowired
    private HealthCheckProcessor processor;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private HealthCheckResultRepository resultRepository;
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private StatusEventRepository statusEventRepository;

    // A monitor with scripted responses, so we can fake a service getting slow and then recovering.
    private static class ScriptedMonitor implements ServiceMonitor {
        private final Deque<HealthCheckResult> scripted = new ArrayDeque<>();

        void enqueue(boolean reachable, int httpStatus, long responseTimeMs) {
            scripted.add(new HealthCheckResult(reachable, httpStatus, responseTimeMs, "scripted"));
        }

        @Override
        public HealthCheckResult check(MonitoredService service) {
            return scripted.poll();
        }
    }

    @Test
    void fullLifecycle_thenAcknowledgeAndResolve() {
        // Register a service in the real database (it gets a generated UUID).
        MonitoredService payment = serviceRepository.save(
                new MonitoredService("Payment", "http://localhost:8081/health", MonitorType.MOCK));

        // Script five checks: fast, slow, unreachable, then two healthy checks.
        ScriptedMonitor monitor = new ScriptedMonitor();
        monitor.enqueue(true, 200, 120);    // healthy
        monitor.enqueue(true, 200, 1500);   // slow  -> degraded
        monitor.enqueue(false, 0, 5000);    // gone  -> down
        monitor.enqueue(true, 200, 130);    // back  -> recovered
        monitor.enqueue(true, 200, 110);    // stable -> healthy

        for (int i = 1; i <= 5; i++) {
            new RunHealthCheckCommand(payment, monitor, processor, resultRepository, serviceRepository)
                    .execute();
            ServiceStatus now = serviceRepository.findById(payment.getId()).orElseThrow().getCurrentStatus();
            System.out.println(">> after check " + i + " -> status = " + now);
        }

        // After the round-trip the service should be healthy again.
        ServiceStatus finalStatus =
                serviceRepository.findById(payment.getId()).orElseThrow().getCurrentStatus();
        assertEquals(ServiceStatus.HEALTHY, finalStatus);

        // Five checks -> five persisted health-check rows.
        assertEquals(5, resultRepository.findByService_IdOrderByCheckedAtDesc(payment.getId()).size());

        // UNKNOWN->HEALTHY->DEGRADED->DOWN->RECOVERED->HEALTHY = 5 events.
        List<StatusEvent> events =
                statusEventRepository.findByService_IdOrderByOccurredAtDesc(payment.getId());
        assertEquals(5, events.size());
        System.out.println(">> status events recorded: " + events.size());

        // Alerts: one WARNING (degraded) + one CRITICAL (down); both auto-resolved on recovery.
        List<Alert> alerts = alertRepository.findByService_IdOrderByCreatedAtDesc(payment.getId());
        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().allMatch(a -> a.getStatus() == AlertStatus.RESOLVED));
        System.out.println(">> alerts raised: " + alerts.size() + " (both auto-resolved on recovery)");

        // now cause another outage so we have an open alert to acknowledge and resolve
        monitor.enqueue(false, 0, 5000);
        new RunHealthCheckCommand(payment, monitor, processor, resultRepository, serviceRepository)
                .execute();
        Alert open = alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.OPEN).get(0);

        new AcknowledgeAlertCommand(open.getId(), "meet", alertRepository).execute();
        assertEquals(AlertStatus.ACKNOWLEDGED,
                alertRepository.findById(open.getId()).orElseThrow().getStatus());

        new ResolveAlertCommand(open.getId(), alertRepository).execute();
        assertEquals(AlertStatus.RESOLVED,
                alertRepository.findById(open.getId()).orElseThrow().getStatus());

        int incidentCountBeforeReopen =
                alertRepository.findByService_IdOrderByCreatedAtDesc(payment.getId()).size();
        monitor.enqueue(false, 0, 5000);
        new RunHealthCheckCommand(payment, monitor, processor, resultRepository, serviceRepository)
                .execute();

        assertEquals(AlertStatus.OPEN,
                alertRepository.findById(open.getId()).orElseThrow().getStatus());
        assertEquals(incidentCountBeforeReopen,
                alertRepository.findByService_IdOrderByCreatedAtDesc(payment.getId()).size());

        System.out.println(">> command flow OK: resolved alert reopened without creating a duplicate");
    }
}
