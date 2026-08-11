package com.cloudpulse.handler;

import com.cloudpulse.lifecycle.ServiceStateMachine;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.model.MonitoredService;
import com.cloudpulse.model.ServiceStatus;
import com.cloudpulse.model.StatusEvent;
import com.cloudpulse.observer.StatusEventPublisher;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Step 4: check if the status actually changed since the last check.
// A change creates an event and updates the service. An unchanged result still
// continues to the alert-policy handler so failure streaks and reopening work.
public class StatusChangeHandler extends ResultHandler {

    private static final Logger log = LoggerFactory.getLogger(StatusChangeHandler.class);

    private final StatusEventRepository statusEventRepository;
    private final MonitoredServiceRepository monitoredServiceRepository;
    private final ServiceStateMachine stateMachine;
    private final StatusEventPublisher statusEventPublisher;

    public StatusChangeHandler(StatusEventRepository statusEventRepository,
                               MonitoredServiceRepository monitoredServiceRepository,
                               ServiceStateMachine stateMachine,
                               StatusEventPublisher statusEventPublisher) {
        this.statusEventRepository = statusEventRepository;
        this.monitoredServiceRepository = monitoredServiceRepository;
        this.stateMachine = stateMachine;
        this.statusEventPublisher = statusEventPublisher;
    }

    @Override
    public void handle(HealthCheckResult result) {
        MonitoredService service = result.getService();
        ServiceStatus previous = service.getCurrentStatus();
        ServiceStatus observed = result.getEvaluatedStatus();

        if (observed == null) {
            return;
        }

        ServiceStatus current = stateMachine.transition(previous, observed);
        result.setEvaluatedStatus(current);

        // No lifecycle change means no event or duplicate alert.
        if (current == previous) {
            // Alert policy still needs every result so it can advance a pending
            // failure streak or reopen an incident that was resolved too early.
            passToNext(result);
            return;
        }

        String reason = "Status changed from " + previous + " to " + current
                + " (response " + result.getResponseTimeMs() + "ms, http " + result.getHttpStatus() + ")";
        StatusEvent event = statusEventRepository.save(new StatusEvent(service, previous, current, reason));

        service.setCurrentStatus(current);
        monitoredServiceRepository.save(service);

        log.info("Service '{}' status changed {} -> {}", service.getName(), previous, current);

        // Notify the Observer, but only after this check's transaction commits, so a
        // failing subscriber can't roll back a real health check. Outside a transaction
        // (like a plain unit test) there's nothing to wait for, so publish immediately.
        publishAfterCommit(event);

        passToNext(result);
    }

    private void publishAfterCommit(StatusEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublish(event);
                }
            });
        } else {
            safePublish(event);
        }
    }

    // A subscriber failure must never disrupt the already-committed health check, so
    // swallow and log it here. (Isolating one subscriber from another is the publisher's job.)
    private void safePublish(StatusEvent event) {
        try {
            statusEventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to publish status event {}", event.getId(), e);
        }
    }
}
