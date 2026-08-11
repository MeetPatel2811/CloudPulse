# CloudPulse — Final Project Milestone 2 Progress Update

**Course:** CSYE 6300  
**Group:** 3 — CloudPulse Team  
**Submission date:** July 27, 2026  
**Private repository:** [CloudPulse GitHub Repository](https://github.com/KathaPatel29/csye6300-summer26-group3-final-project)

> The repository is private. All four team members must have access before submission.

## 1. Project Group Number and Name

**Group number:** 3  
**Group name:** CloudPulse Team

**Team members:**

- Katha Patel
- Ashwin S Thankachan
- Meet Patel
- Pavithra Prasad

## 2. Project Topic and Name

**CloudPulse — Cloud Service Health Monitoring and Alert Dashboard**

CloudPulse is a student-scale observability application that monitors registered services, records their availability and response time, evaluates their health, tracks lifecycle transitions, and creates alerts when services become degraded or unavailable. The system supports both scheduled and manual health checks and demonstrates multiple object-oriented design patterns through a working Spring Boot backend.

## 3. Technology Stack

### Implemented in Milestone 2

- Java 22
- Spring Boot 3.3.4
- Spring Web and REST controllers
- Spring Scheduling
- Spring Data JPA
- Jakarta Bean Validation
- Maven and Maven Wrapper
- H2 in-memory development database
- PostgreSQL runtime driver and Docker profile configuration
- Springdoc OpenAPI and Swagger UI
- JUnit 5
- Mockito
- Spring Boot integration testing and MockMvc
- GitHub pull requests and GitHub Actions

### Planned for Milestone 3

- React
- TypeScript
- Vite
- Docker Compose
- PostgreSQL container deployment

The React frontend and Docker Compose environment are not yet implemented and are therefore not presented as Milestone 2 functionality.

## 4. Functionalities Implemented for Milestone 2

### Service management

- Register a monitored service through the REST API.
- List all registered services.
- Retrieve a service by its identifier.
- Enable or disable monitoring for a service.
- Run an immediate manual health check.
- Select either an HTTP monitor or mock monitor based on monitor type.

### Scheduled monitoring

- Run automatic checks for all enabled services.
- Use a configurable global scheduling delay, currently 15 seconds.
- Continue checking other services when one service fails.
- Apply logging and retry behavior to scheduled monitors.

### Health-check execution

- Call real HTTP health endpoints.
- Measure response time.
- Record reachability and HTTP status.
- Normalize standard and legacy response formats.
- Support three runnable demo services:
  - Standard response service
  - Legacy response service
  - Flaky service with healthy, slow, and failing behavior

### Health evaluation and lifecycle

- Evaluate results using Normal or Strict policies.
- Classify services as Unknown, Healthy, Degraded, Down, or Recovered.
- Use different response-time thresholds for Normal and Strict evaluation.
- Record status transitions in chronological order.
- Model recovery as Down or Degraded to Recovered, followed by Recovered to Healthy.

### Alert processing

- Create warning alerts for degraded services.
- Create critical alerts for down services.
- Resolve existing alerts when a service recovers.
- List all alerts or filter them by status.
- Acknowledge an alert with an operator name.
- Manually resolve an alert.

### Persistence and API access

- Persist monitored services, health-check results, status events, and alerts.
- Use H2 for local development and demonstrations.
- Provide a PostgreSQL profile for later container deployment.
- Explore and execute REST endpoints through Swagger UI.

### Testing and continuous integration

- Unit tests for monitor adapters, factories, decorators, commands, handlers, Strategy, State, Observer components, and the scheduler.
- Spring integration tests for the complete processing and alert workflow.
- MockMvc integration tests for service and alert controllers.
- **Current verified result:** 55 tests passed, 0 failures, and 0 errors.

## 5. Design Patterns Implemented

### 5.1 Factory Method

**Problem solved:** The monitoring workflow should select the appropriate monitor without depending directly on concrete monitor construction.

The scheduler resolves a creator by monitor type:

```java
private MonitorCreator resolveCreator(MonitorType monitorType) {
    return switch (monitorType) {
        case HTTP -> httpMonitorCreator;
        case MOCK -> mockMonitorCreator;
    };
}
```

The selected creator returns a common `ServiceMonitor`, allowing additional monitor types to be introduced without changing the health-processing pipeline.

### 5.2 Adapter

**Problem solved:** External services may return different health-response formats.

```java
public interface HealthResponseAdapter {
    HealthCheckResult adapt(
            String rawResponse,
            int httpStatus,
            long responseTimeMs
    );

    boolean supports(String rawResponse);
}
```

`StandardHealthAdapter` handles responses containing fields such as `status` and `responseTimeMs`. `LegacyHealthAdapter` handles responses containing `result` and `latency`. Both produce the same `HealthCheckResult` domain model.

### 5.3 Decorator

**Problem solved:** Logging and retry behavior should be added without modifying each concrete monitor.

```java
ServiceMonitor baseMonitor = creator.createMonitor();
return new RetryMonitorDecorator(
        new LoggingMonitorDecorator(baseMonitor)
);
```

Scheduled checks use the same `ServiceMonitor` interface while dynamically adding logging and retry behavior.

### 5.4 Strategy

**Problem solved:** Different services or environments may use different health-evaluation policies.

```java
public interface HealthEvaluationStrategy {
    EvaluationPolicy policy();

    ServiceStatus evaluate(HealthCheckSnapshot snapshot);
}
```

`NormalHealthStrategy` marks a response as degraded after 1,000 ms. `StrictHealthStrategy` uses a 500 ms threshold. Both mark unreachable responses and HTTP errors as down.

The selected policy is read from each monitored service:

```java
ServiceStatus status = evaluationService.evaluate(
        result.getService().getActiveEvaluationStrategy(),
        snapshot
);
```

### 5.5 State

**Problem solved:** Recovery behavior depends on the service’s current lifecycle state, not only the latest observed result.

```java
public ServiceStatus transition(
        ServiceStatus currentStatus,
        ServiceStatus observedStatus
) {
    ServiceState currentState = states.get(currentStatus);
    if (currentState == null) {
        throw new IllegalArgumentException(
                "No state registered for " + currentStatus
        );
    }
    return currentState.transition(observedStatus);
}
```

Concrete states implement the allowed behavior for Unknown, Healthy, Degraded, Down, and Recovered services.

### 5.6 Chain of Responsibility

**Problem solved:** Health-check processing contains multiple independent stages that should remain ordered, modular, and testable.

```java
ResultHandler validation = new ResponseValidationHandler();
validation.setNext(new AvailabilityHandler())
          .setNext(new LatencyHandler(evaluationService))
          .setNext(new StatusChangeHandler(
                  statusEventRepository,
                  monitoredServiceRepository,
                  stateMachine
          ))
          .setNext(new AlertCreationHandler(alertRepository));
```

Each handler performs one responsibility and either passes the result forward or stops processing when no further action is required.

### 5.7 Command

**Problem solved:** Monitoring and alert operations need consistent execution boundaries and reusable invocation.

```java
public interface MonitoringCommand {
    void execute();
}
```

Commands include:

- `RunHealthCheckCommand`
- `EnableMonitoringCommand`
- `DisableMonitoringCommand`
- `AcknowledgeAlertCommand`
- `ResolveAlertCommand`

The scheduler creates and executes the same health-check command used by the rest of the application:

```java
RunHealthCheckCommand command =
        commandFactory.create(service, monitor);
command.execute();
```

### 5.8 Observer

**Problem solved:** Multiple components should react to a status event without the publisher depending on concrete subscribers.

```java
public void publish(StatusEvent event) {
    for (EventSubscriber subscriber : subscribers) {
        subscriber.onEvent(event);
    }
}
```

The current implementation includes:

- `DashboardUpdateObserver`
- `AlertObserver`
- `EventHistoryObserver`

The publisher and observers are implemented and unit-tested. Integration with the production `StatusChangeHandler` remains planned work. The Milestone 2 submission therefore describes Observer as a tested prototype rather than claiming that it currently drives the live alert or dashboard workflow.

### 5.9 MVC Architecture

**Problem solved:** HTTP request handling should remain separate from domain models, processing behavior, and persistence.

```java
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @PostMapping
    public MonitoredService registerService(
            @Valid @RequestBody RegisterServiceRequest request
    ) {
        MonitoredService service = new MonitoredService(
                request.name(),
                request.healthUrl(),
                request.monitorType()
        );
        return serviceRepository.save(service);
    }
}
```

Spring REST controllers form the Controller layer, entities and services form the Model and business layers, and Swagger currently acts as the interactive client. The React View is planned for Milestone 3.

## 6. Current System Demonstration

A typical running demonstration follows this sequence:

1. Start the Standard, Legacy, and Flaky demo services.
2. Start the CloudPulse backend.
3. Register all three services through Swagger.
4. Allow the scheduler to run checks every 15 seconds.
5. Observe accumulating rows in the health-check table.
6. Observe status transitions such as Healthy to Degraded, Down, Recovered, and Healthy.
7. Observe warning or critical alerts and their automatic resolution after recovery.

The development database exposes the following tables:

- `MONITORED_SERVICE`
- `HEALTH_CHECK_RESULT`
- `STATUS_EVENT`
- `ALERT`

## 7. Functionalities Planned for Milestone 3

- Integrate `StatusEventPublisher` with real status transitions.
- Register Observer subscribers through Spring.
- Add a React, TypeScript, and Vite dashboard.
- Display service status cards, alert lists, and event history.
- Add API endpoints for health-check and status-event history.
- Add an endpoint for changing a service’s evaluation policy.
- Add service editing and deletion.
- Respect each service’s individual check interval.
- Integrate the metrics monitor decorator.
- Add Docker Compose and run the backend with PostgreSQL.
- Add browser-level frontend testing.
- Add complete end-to-end tests against live demo services.
- Improve production error responses and API validation.

## 8. Team Contributions

| Team member | Milestone 2 contribution |
|---|---|
| **Katha Patel** | Built the backend monitoring foundation, domain models, repositories, HTTP and mock monitors, Standard and Legacy adapters, Factory Method creators, Logging/Retry/Metrics decorators, three demo services, Maven wrappers, and the scheduled health-check workflow with a command factory. |
| **Ashwin S Thankachan** | Implemented Normal and Strict Strategy evaluation, the service State lifecycle, recovery transitions, integration with the processing chain, focused unit tests, integration-test updates, and CI workflow stabilization while the frontend is absent. |
| **Meet Patel** | Implemented the Chain of Responsibility processing workflow, Command implementations for health checks and alert actions, alert generation and resolution behavior, repository interactions, and the end-to-end processing workflow integration test. |
| **Pavithra Prasad** | Implemented the Observer publisher/subscriber structure and Observer unit test, Spring MVC REST controllers for services and alerts, request/response models, validation, error handling, and MockMvc controller integration tests. |

The team also collaborated on integration review, pull-request validation, continuous-integration checks, and alignment between shared components.

## 9. Repository and Submission Confirmation

**Private repository URL:**  
https://github.com/KathaPatel29/csye6300-summer26-group3-final-project
