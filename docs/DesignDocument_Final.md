CLOUDPULSE | DESIGN DOCUMENT • FINAL SUBMISSION
CSYE 6300 • SUMMER 2026 • GROUP 3

# CloudPulse
## Design Document — Final Submission

**Project:** Cloud Service Health Monitoring and Alert Dashboard
**Team:** Katha Patel • Ashwin S. Thankachan • Meet Patel • Pavithra Prasad
**Repository:** github.com/KathaPatel29/csye6300-summer26-group3-final-project

## 1. Project Group Number and Name

Group number: 3
Group name: CloudPulse Team
- Katha Patel
- Ashwin S Thankachan
- Meet Patel
- Pavithra Prasad

## 2. Project Topic and Name

**CloudPulse — Cloud Service Health Monitoring and Alert Dashboard**

CloudPulse is a student-scale observability application that registers
services, runs scheduled or manual health checks (HTTP, keyword-content,
or a controllable mock), normalizes external response formats, evaluates
health against per-service thresholds, records lifecycle transitions,
raises and reopens alerts, suppresses alerting during scheduled
maintenance, sends webhook notifications, tracks incidents, and presents
current and historical service state through a React dashboard and a
read-only public status page.

## 3. Technology Stack

| Layer | Technologies | Final-submission use |
|---|---|---|
| Backend | Java 22; Spring Boot 3.3.4; Spring Web; Scheduling; Data JPA; Validation | REST API, monitoring workflow, scheduling, domain rules, persistence |
| Frontend | React 19; TypeScript 5.7; Vite 6; Tailwind CSS 4 | Full dashboard, service directory, incident workspace, notification center, public status page |
| Data | H2; PostgreSQL 16 | H2 development/testing; PostgreSQL Docker profile |
| Infrastructure | Docker Compose; backend/frontend Dockerfiles | Compose configuration for backend, frontend, PostgreSQL, and all three demo services |
| Testing | JUnit 5; Mockito; Spring Boot Test; MockMvc; Vitest; React Testing Library; Playwright | Unit, controller, scheduler, workflow, and Observer integration tests; frontend component tests; browser end-to-end tests |
| Documentation/CI | OpenAPI/Swagger; GitHub Actions; Maven/npm | API discovery and backend/frontend verification |

## 4. Functionalities Implemented — Final Submission

| Area | Verified progress | Repository evidence |
|---|---|---|
| React dashboard | Service directory with status badges, summary cards, group/tag filtering, loading/error/empty states, responsive Tailwind UI. | `frontend/src/App.tsx`, `ServiceDirectory.tsx` |
| Service CRUD | Register (URL probe, monitor type, check interval, group, tags), edit, and delete services; deletion blocked with HTTP 409 when history exists. | `ServiceForm`, `AddMonitorWizard`, `ServiceController` |
| Monitor types | HTTP, MOCK, and KEYWORD monitor types share the same Factory Method, Decorator, and Chain of Responsibility pipeline. | `KeywordServiceMonitor`, `KeywordMonitorCreator` |
| Reliability policy | Custom per-service latency threshold and configurable "alert after N unhealthy checks", with incident reopening. | `ReliabilityPolicyController`, `AlertCreationHandler` |
| Maintenance windows | Scheduled start/end windows suppress alerting for a service while active, without affecting other services. | `MaintenanceWindowController`, `AlertCreationHandler` |
| SSL certificate monitoring | On-demand TLS handshake reports certificate expiry status for HTTPS targets. | `SslCertificateController`, `SslCertificateInspector` |
| SLO / error budget | Per-service availability target with an error-budget view in the dashboard. | `SloTargetController`, `SloDashboard.tsx` |
| Alerts and incidents | Alert list with acknowledge/resolve; incident workspace with owner assignment, notes, and activity timeline. | `IncidentController`, `IncidentWorkspace.tsx` |
| Notifications | Slack/Teams/Discord webhook targets with delivery history, sent automatically on alert-worthy transitions. | `WebhookController`, `WebhookNotifier`, `NotificationCenter.tsx` |
| History and policy APIs | Health-check history, status-event history, and NORMAL/STRICT evaluation-policy timelines in the dashboard. | `HealthCheckHistoryController`, `HealthCheckTimeline.tsx` |
| Public status page | Unauthenticated, read-only summary of current service status at `/status`. | `PublicStatusController`, `PublicStatusPage.tsx` |
| Reporting | CSV export of health-check and incident data from the dashboard. | `frontend/src/lib/csvReport.ts` |
| Live Observer flow | Spring registers all subscribers, including the webhook notifier; committed status transitions publish events; one subscriber failure does not stop others. | `ObserverRegistrar`, integration tests |
| Deployment | Docker Compose starts PostgreSQL, backend, frontend, and all three demo services with health-gated startup and seed data. | `docker-compose.yml`, `DemoDataSeeder` |
| Verification | 170+ backend tests, full frontend component-test suite, and a Playwright end-to-end test, all passing. | `backend/target/surefire-reports`, `frontend/e2e` |

## 5. Design Patterns Implemented — Code Snippets

### 5.1 Factory Method
**Problem solved:** Selects HTTP, mock, or keyword-content monitors without coupling callers to concrete construction.
**Current participants:** `MonitorCreator`, `HttpMonitorCreator`, `MockMonitorCreator`, `KeywordMonitorCreator`, `ServiceMonitor`

Factory Method excerpt — `backend/.../scheduler/HealthCheckScheduler.java`
```java
private MonitorCreator resolveCreator(MonitorType monitorType) {
    return switch (monitorType) {
        case HTTP -> httpMonitorCreator;
        case MOCK -> mockMonitorCreator;
        case KEYWORD -> keywordMonitorCreator;
    };
}
```

### 5.2 Adapter
**Problem solved:** Normalizes standard, legacy, and generic health-response bodies into one `HealthCheckResult` model — and, in the final phase, normalizes three different webhook payload formats behind one contract.
**Current participants:** `HealthResponseAdapter`, `StandardHealthAdapter`, `LegacyHealthAdapter`, `GenericHttpAdapter`; `WebhookPayloadAdapter`, `SlackPayloadAdapter`, `TeamsPayloadAdapter`, `DiscordPayloadAdapter`

Adapter excerpt — `backend/.../notification/WebhookPayloadAdapter.java` + `SlackPayloadAdapter.java`
```java
public interface WebhookPayloadAdapter {
    boolean supports(WebhookProvider provider);
    String buildBody(NotificationMessage message);
}

// Slack incoming webhooks accept a simple {"text": "..."} body.
public String buildBody(NotificationMessage message) {
    return WebhookJson.write(Map.of("text", message.text()));
}
```

### 5.3 Decorator
**Problem solved:** Layers logging, retry, and metrics behavior around a monitor without changing it.
**Current participants:** `MonitorDecorator`, `LoggingMonitorDecorator`, `RetryMonitorDecorator`, `MetricsMonitorDecorator`

Decorator excerpt — `backend/.../scheduler/HealthCheckScheduler.java`
```java
private ServiceMonitor createDecoratedMonitor(MonitoredService service) {
    MonitorCreator creator = resolveCreator(service.getMonitorType());
    ServiceMonitor baseMonitor = creator.createMonitor();
    return new MetricsMonitorDecorator(
        new RetryMonitorDecorator(
            new LoggingMonitorDecorator(baseMonitor)));
}
```

### 5.4 Strategy
**Problem solved:** Allows NORMAL and STRICT latency thresholds to vary by monitored service, with an optional per-service override.
**Current participants:** `HealthEvaluationStrategy`, `NormalHealthStrategy`, `StrictHealthStrategy`, `HealthEvaluationService`

Strategy excerpt — `backend/.../handler/LatencyHandler.java`
```java
ServiceStatus status = evaluationService.evaluate(
    result.getService().getActiveEvaluationStrategy(),
    snapshot,
    result.getService().getLatencyThresholdMs()   // per-service override, or null for the policy default
);
result.setEvaluatedStatus(status);
```

### 5.5 State
**Problem solved:** Controls lifecycle transitions and explicit recovery according to the current service state.
**Current participants:** `ServiceState`, `ServiceStateMachine`, `Unknown/Healthy/Degraded/Down/Recovered` states

State excerpt — `backend/.../lifecycle/DownState.java`
```java
public class DownState implements ServiceState {
    public ServiceStatus status() { return ServiceStatus.DOWN; }

    public ServiceStatus transition(ServiceStatus observedStatus) {
        return observedStatus == ServiceStatus.HEALTHY
            ? ServiceStatus.RECOVERED
            : observedStatus;
    }
}
```

### 5.6 Chain of Responsibility
**Problem solved:** Processes validation, availability, latency, status changes, and threshold-aware alerting as ordered, independent stages.
**Current participants:** `ResultHandler` and five concrete handlers, `HealthCheckProcessor`

Chain of Responsibility excerpt — `backend/.../handler/HealthCheckProcessor.java`
```java
ResultHandler validation = new ResponseValidationHandler();
validation.setNext(new AvailabilityHandler())
    .setNext(new LatencyHandler(evaluationService))
    .setNext(new StatusChangeHandler(
        statusEventRepository, monitoredServiceRepository,
        stateMachine, statusEventPublisher))
    .setNext(new AlertCreationHandler(
        alertRepository, monitoredServiceRepository, maintenanceWindowRepository));
this.chainHead = validation;
```

### 5.7 Command
**Problem solved:** Encapsulates checks, monitoring toggles, alert acknowledgement/resolution, and incident ownership/notes behind consistent execution boundaries.
**Current participants:** `MonitoringCommand` and seven concrete commands, including the final phase's `AssignIncidentOwnerCommand` and `AddIncidentNoteCommand`

Command excerpt — `backend/.../command/RunHealthCheckCommand.java`
```java
public interface MonitoringCommand {
    void execute();
}

public void execute() {
    HealthCheckResult result = monitor.check(service);
    result.setService(service);
    processor.process(result);
    resultRepository.save(result);
    service.setLastCheckedAt(Instant.now());
    serviceRepository.save(service);
}
```

### 5.8 Observer
**Problem solved:** Publishes committed status events to isolated dashboard, alert, history, and (final phase) webhook-notification subscribers.
**Current participants:** `StatusEventPublisher`, `ObserverRegistrar`, `EventSubscriber` implementations including `WebhookNotifier`

Observer excerpt — `backend/.../observer/ObserverRegistrar.java`
```java
@PostConstruct
void registerObservers() {
    publisher.register(dashboardUpdateObserver);
    publisher.register(alertObserver);
    publisher.register(eventHistoryObserver);
    publisher.register(webhookNotifier);
}

// StatusChangeHandler calls publisher after commit;
// StatusEventPublisher isolates failures per subscriber.
```

### 5.9 MVC
**Problem solved:** Separates the React View, Spring REST controllers, domain/services, and repositories.
**Current participants:** React components, 20+ REST controllers (`ServiceController`, `AlertController`, `IncidentController`, `MaintenanceWindowController`, `WebhookController`, etc.), domain model, repositories

MVC excerpt — `frontend/src/api/services.ts` + `ServiceController.java`
```java
export function updateService(id: string, request: RegisterServiceRequest) {
  return put<MonitoredService>(`/services/${id}`, request)
}

@PutMapping("/{id}")
public MonitoredService updateService(
        @PathVariable UUID id,
        @Valid @RequestBody RegisterServiceRequest request) {
    MonitoredService service = findServiceOrThrow(id);
    service.setName(request.name());
    service.setHealthUrl(request.healthUrl());
    service.setMonitorType(request.monitorType());
    return serviceRepository.save(service);
}
```

## 6. Current System Demonstration

1. Start everything with `docker compose up --build` (PostgreSQL, backend, frontend, and all three demo services), or run the backend with its H2 development profile and start pieces individually.
2. Open the dashboard at `http://localhost:5173` — seed data (Demo Standard/Legacy/Flaky) is already registered.
3. Register a new monitor (HTTP, MOCK, or KEYWORD), with a service group, tags, and a custom check interval.
4. Open a service's detail view: adjust its evaluation policy, reliability policy (custom latency/failure thresholds), SLO target, and schedule a maintenance window.
5. Trigger a failure past the configured threshold, watch the alert open in the Incident Center, assign an owner, add a note, then resolve it and re-break it to see it reopen.
6. Register a Slack/Teams/Discord webhook target in the Notification Center and confirm a delivery attempt is recorded (success or failure) after the next alert.
7. Open `http://localhost:5173/status` for the public, read-only status view, and export a CSV report from the dashboard.
8. Edit a service before it has history, and demonstrate the protected delete behavior after monitoring history exists.

## 7. Team Contributions — Final Phase

| Member | Contribution |
|---|---|
| Katha Patel | Docker Compose for the full stack (frontend and all three demo services), startup seed data, configurable check intervals, maintenance windows with alert suppression, TLS certificate expiry monitoring, keyword-content monitoring (new `MonitorType`) |
| Ashwin S Thankachan | Per-service custom latency thresholds, configurable alert-after-N-failures with incident reopening, health-check history timeline UI |
| Meet Patel | Alert view UI, Slack/Teams/Discord webhook notifications with delivery history, incident owner assignment and notes with activity timeline |
| Pavithra Prasad | Frontend component tests, Playwright end-to-end coverage, accessibility pass, public status page, service groups/tags with directory filtering, SLO/error-budget dashboard, CSV report export, incident workspace and notification center UI |
