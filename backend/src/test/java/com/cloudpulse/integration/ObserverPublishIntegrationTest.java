package com.cloudpulse.integration;

import com.cloudpulse.handler.StatusChangeHandler;
import com.cloudpulse.lifecycle.ServiceStateMachine;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitorType;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.observer.AlertObserver;
import com.cloudpulse.observer.DashboardUpdateObserver;
import com.cloudpulse.observer.EventHistoryObserver;
import com.cloudpulse.observer.StatusEventPublisher;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Proves the core M3 behavior: StatusChangeHandler notifies the Observer only AFTER the
// transaction commits, and not at all if it rolls back. Uses a fresh publisher with a
// capturing subscriber so it doesn't touch the shared publisher bean.
@SpringBootTest
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class ObserverPublishIntegrationTest {

    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private StatusEventRepository statusEventRepository;
    @Autowired
    private ServiceStateMachine stateMachine;
    @Autowired
    private PlatformTransactionManager transactionManager;

    // The real, Spring-wired publisher with the three observers registered on startup
    // (via ObserverRegistrar), plus the three observer beans themselves, so we can assert
    // each one actually received the event.
    @Autowired
    private StatusEventPublisher wiredPublisher;
    @Autowired
    private DashboardUpdateObserver dashboardUpdateObserver;
    @Autowired
    private AlertObserver alertObserver;
    @Autowired
    private EventHistoryObserver eventHistoryObserver;

    private final List<StatusEvent> received = new ArrayList<>();
    private StatusChangeHandler handler;

    @BeforeEach
    void setUp() {
        received.clear();
        StatusEventPublisher publisher = new StatusEventPublisher();
        publisher.register(received::add);
        handler = new StatusChangeHandler(
                statusEventRepository, serviceRepository, stateMachine, publisher);
    }

    private MonitoredService healthyService(String name, String url) {
        MonitoredService service = serviceRepository.save(
                new MonitoredService(name, url, MonitorType.MOCK));
        service.setCurrentStatus(ServiceStatus.HEALTHY);
        return serviceRepository.save(service);
    }

    // A reachable-but-slow result, which is a HEALTHY -> DEGRADED transition (a real change).
    private HealthCheckResult degradedCheckFor(MonitoredService service) {
        HealthCheckResult result = new HealthCheckResult(true, 200, 1500, "slow");
        result.setService(service);
        result.setEvaluatedStatus(ServiceStatus.DEGRADED);
        return result;
    }

    @Test
    void publishesOnlyAfterCommit() {
        MonitoredService service = healthyService("Payment", "http://localhost:8081/health");

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> handler.handle(degradedCheckFor(service)));

        assertEquals(1, received.size());
        assertEquals(ServiceStatus.DEGRADED, received.get(0).getNewStatus());
    }

    @Test
    void doesNotPublishWhenTransactionRollsBack() {
        MonitoredService service = healthyService("User", "http://localhost:8082/health");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            handler.handle(degradedCheckFor(service));
            status.setRollbackOnly();
        });

        assertTrue(received.isEmpty());
    }

    // Proves the full wiring: a real status change published through the actual Spring bean
    // reaches all three registered observers AFTER commit. Each observer reads
    // event.getService().getName() in onEvent(); the service association is @ManyToOne(LAZY),
    // so if it were an uninitialized proxy here (session already closed after commit) the read
    // would throw and — thanks to the publisher's per-subscriber isolation — the observer would
    // silently record nothing. Asserting each observer captured this event by name therefore
    // also proves no LazyInitializationException occurred.
    @Test
    void publishReachesAllThreeObserversAfterCommitWithoutLazyInitException() {
        // Unique name so we can find this event among any others the shared observers hold.
        String serviceName = "Inventory-" + java.util.UUID.randomUUID();
        MonitoredService service = healthyService(serviceName, "http://localhost:8083/health");

        StatusChangeHandler wiredHandler = new StatusChangeHandler(
                statusEventRepository, serviceRepository, stateMachine, wiredPublisher);

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> wiredHandler.handle(degradedCheckFor(service)));

        // DashboardUpdateObserver keys by service name — a non-null entry means it read
        // getName() successfully after commit.
        StatusEvent dashboardEvent = dashboardUpdateObserver.getLatestStatus(serviceName);
        assertNotNull(dashboardEvent, "dashboard observer should have recorded the event");
        assertEquals(ServiceStatus.DEGRADED, dashboardEvent.getNewStatus());

        assertTrue(
                alertObserver.getTriggeringEvents().stream()
                        .anyMatch(e -> serviceName.equals(e.getService().getName())),
                "alert observer should have recorded the DEGRADED transition");
        assertTrue(
                eventHistoryObserver.getHistory().stream()
                        .anyMatch(e -> serviceName.equals(e.getService().getName())),
                "history observer should have recorded the event");
    }
}
