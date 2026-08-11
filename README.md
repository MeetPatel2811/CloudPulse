# CloudPulse — Cloud Service Health Monitoring & Alert Dashboard

CloudPulse is a full-stack Java application that monitors the availability and
response time of application services. It periodically performs real HTTP
health checks (including keyword-content checks and TLS certificate expiry)
against registered services, classifies each service as **Unknown, Healthy,
Degraded, Down, or Recovered**, raises threshold-aware alerts on unhealthy
transitions, suppresses alerting during scheduled maintenance windows, and
displays everything through a React dashboard — plus a read-only public
status page for customers.

> Student-scale observability project for **CSYE 6300 — Summer 2026, Group 3**.
> It is intentionally a learning project, not a replacement for commercial
> platforms such as Datadog or New Relic.

## Team — Group 3 (CloudPulse Team)

| Member | Core area (original design patterns) | Final-phase additions |
|---|---|---|
| Katha Patel | Monitoring engine & demo services (Factory Method, Adapter, Decorator) | Docker Compose for the full stack, seed data, configurable check intervals, maintenance windows, SSL certificate monitoring, keyword monitoring |
| Ashwin S Thankachan | Health evaluation & lifecycle (Strategy, State) | Custom latency thresholds, alert-after-N-failures, incident reopening, health-check history view |
| Meet Patel | Processing & alert workflow (Chain of Responsibility, Command) | Alert view UI, Slack/Teams/Discord webhook notifications + delivery history, incident owner assignment & notes |
| Pavithra Prasad | React dashboard & service registration (Observer, MVC) | Frontend tests, browser e2e tests, accessibility, public status page, service groups/tags, SLO dashboard, CSV export |

## Tech Stack

- **Backend:** Java SE JDK 22, Spring Boot, Spring Web, Spring Scheduler, Spring Data JPA, Maven
- **Frontend:** React, TypeScript, Vite
- **Data & Infra:** PostgreSQL, Docker Compose
- **Testing & Docs:** JUnit 5, Mockito, Spring Boot integration tests, OpenAPI/Swagger

## Project Structure

```
csye6300-summer26-group3-final-project/
├─ docs/               # Milestone proposals, diagrams
├─ backend/            # Spring Boot API (Maven, includes ./mvnw), + Dockerfile
├─ demo-services/      # Three minimal Spring Boot services used for demos, each with its own Dockerfile
│  ├─ demo-standard/   # {"status":"UP", "responseTimeMs": ...} shape, port 8081
│  ├─ demo-legacy/      # {"result":"ok", "latency": ...} shape, port 8082
│  └─ demo-flaky/       # randomly slow / down / healthy, port 8083
├─ frontend/           # React + TypeScript + Vite dashboard (service management UI), + Dockerfile/nginx.conf
└─ docker-compose.yml  # Postgres + backend + frontend + all three demo services
```

> See [`docs/milestone-1-proposal.md`](docs/milestone-1-proposal.md) for the
> original plan and [`docs/DesignDocument_Final.md`](docs/DesignDocument_Final.md)
> for the as-built architecture, updated UML/ER diagrams, and design patterns.

## Getting Started

### Prerequisites
- JDK 22 (or later)
- That's it — you do **not** need Maven installed. Every module
  (`backend/`, each `demo-services/*`) includes its own wrapper
  (`./mvnw` on macOS/Linux, `mvnw.cmd` on Windows), which downloads the
  correct Maven version on first run.

### 1. Run the backend
```bash
cd backend
./mvnw spring-boot:run
```
Runs against an in-memory H2 database by default (`dev` profile) — no
Postgres or Docker required to try it locally. On first startup it also
seeds three demo services (pointing at `localhost:8081/8082/8083`) so the
dashboard isn't empty; this is skipped once any service already exists.
- API: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 2. (Optional) Run a demo service to see monitoring in action
Each demo service exposes a `/health` endpoint the backend's `HttpServiceMonitor`
can poll. Start whichever ones you want to test against, each in its own terminal:
```bash
cd demo-services/demo-standard && ./mvnw spring-boot:run   # port 8081
cd demo-services/demo-legacy   && ./mvnw spring-boot:run   # port 8082
cd demo-services/demo-flaky    && ./mvnw spring-boot:run   # port 8083
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
- Dashboard: `http://localhost:5173` (talks to the backend at `http://localhost:8080`)
- Public status page (read-only, no login): `http://localhost:5173/status`
- Currently supports in the dashboard:
  - Register / edit / delete services, with a URL probe step, service group,
    tags, and monitor type (`HTTP`, `MOCK`, or `KEYWORD`)
  - Filter the service list by status, group, or tag
  - Configurable check interval (15s / 30s / 1m / 5m)
  - NORMAL/STRICT evaluation policy, plus a per-service custom latency
    threshold and "alert after N unhealthy checks" setting
  - Manual "run check now" and enable/disable controls
  - Per-service health-check history and status-event timelines
  - SSL certificate expiry status for HTTPS targets (with manual recheck)
  - Scheduled maintenance windows that suppress alerting while active
  - SLO / availability target per service, with an error-budget view
  - Alert view with acknowledge/resolve, and incidents reopen automatically
    if the same failure streak continues after being resolved
  - Incident workspace: assign an owner, add investigation notes, and view
    the activity timeline for an incident
  - Notification center: register Slack/Teams/Discord webhook targets and
    view delivery history (both under the Incidents tab)
  - CSV export of health-check/incident reports

### 4. (Alternative) Run everything with Docker Compose
This builds and starts the backend, dashboard, and all three demo services
alongside PostgreSQL, using the `docker` Spring profile instead of the
in-memory H2 database:
```bash
docker compose up --build
```
- Dashboard: `http://localhost:5173` (Nginx-served build, proxies `/api` to
  the backend container)
- API: `http://localhost:8080`
- Demo services: `http://localhost:8081` (standard), `8082` (legacy),
  `8083` (flaky)
- PostgreSQL: `localhost:5432` (db `cloudpulse`, user/pass `cloudpulse`/`cloudpulse`)

On first startup, with an empty database, the backend seeds three services
pointing at the demo containers (Demo Standard, Demo Legacy, Demo Flaky) so
the dashboard has data to show right away; the seeder is skipped on later
restarts once any service already exists.

Stop everything with `docker compose down` (add `-v` to also drop the
Postgres data volume).

## Testing

```bash
cd backend && ./mvnw test        # 170+ JUnit/Mockito/MockMvc tests
cd frontend && npm test          # Vitest + React Testing Library
cd frontend && npm run test:e2e  # Playwright; auto-starts the Vite dev server
```

## Documentation

- [Milestone 1 Proposal](docs/milestone-1-proposal.md) — original problem
  statement, UML/ER diagrams, and planned design patterns.
- [Milestone 2 Progress Update](docs/CloudPulse_Milestone2.md) and
  [Design Document — Milestone 2](docs/DesignDocument_Milestone2.pdf)
- [Milestone 3 Progress Update](docs/CloudPulse_Milestone3.pdf) and
  [Design Document — Milestone 3](docs/DesignDocument_Milestone3.pdf)
- [Design Document — Final](docs/DesignDocument_Final.md) — as-built
  architecture, updated UML/ER diagrams, and design patterns with current
  code references.

## Repository

https://github.com/KathaPatel29/csye6300-summer26-group3-final-project
