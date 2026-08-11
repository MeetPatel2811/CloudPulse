package com.cloudpulse.handler;

import com.cloudpulse.evaluation.HealthEvaluationService;
import com.cloudpulse.lifecycle.ServiceStateMachine;
import com.cloudpulse.model.HealthCheckResult;
import com.cloudpulse.observer.StatusEventPublisher;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.MaintenanceWindowRepository;
import com.cloudpulse.repository.MonitoredServiceRepository;
import com.cloudpulse.repository.StatusEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Builds the handler chain once and gives one entry point to run it.
// Chain order: validation -> availability -> latency -> status change -> alert.
@Service
public class HealthCheckProcessor {

    private final ResultHandler chainHead;

    public HealthCheckProcessor(StatusEventRepository statusEventRepository,
                                MonitoredServiceRepository monitoredServiceRepository,
                                AlertRepository alertRepository,
                                MaintenanceWindowRepository maintenanceWindowRepository,
                                HealthEvaluationService evaluationService,
                                ServiceStateMachine stateMachine,
                                StatusEventPublisher statusEventPublisher) {
        ResultHandler validation = new ResponseValidationHandler();
        validation.setNext(new AvailabilityHandler())
                  .setNext(new LatencyHandler(evaluationService))
                  .setNext(new StatusChangeHandler(
                          statusEventRepository,
                          monitoredServiceRepository,
                          stateMachine,
                          statusEventPublisher
                  ))
                  .setNext(new AlertCreationHandler(
                          alertRepository, monitoredServiceRepository, maintenanceWindowRepository));
        this.chainHead = validation;
    }

    // Runs the whole chain in one transaction, so the status event, the updated
    // service status, and the alert are all saved together or not at all.
    @Transactional
    public void process(HealthCheckResult result) {
        chainHead.handle(result);
    }
}
