# CloudPulse Frontend

React + TypeScript + Vite dashboard for the CloudPulse backend.

## Prerequisites

- Node.js 20.19+ (or 22.12+)
- The backend running on `http://localhost:8080` (`cd ../backend && ./mvnw spring-boot:run`)

## Run locally

```bash
cd frontend
npm install
npm run dev
```

The dev server runs at `http://localhost:5173` and proxies any `/api/*`
request to `http://localhost:8080` (see `vite.config.ts`), so no CORS
configuration is needed on the backend.

## Build

```bash
npm run build
```

## Tests

Run the React component tests:

```bash
npm test
```

Install Chromium once, then run the Playwright browser workflow:

```bash
npx playwright install chromium
npm run test:e2e
```

The Playwright test uses deterministic API responses and exercises the complete
operator journey: register a monitor, run a health check, observe the generated
incident, and resolve it. It starts the Vite server automatically, so a backend
or Docker environment is not required for this browser-level frontend test.

## Docker

`Dockerfile` builds the production bundle and serves it via Nginx, proxying
`/api/*` to the `backend` container (see `nginx.conf`). It's wired into the
repo-root `docker-compose.yml` as the `frontend` service — run
`docker compose up --build` from the repo root rather than building this
image standalone.

## Project structure

```
src/
  api/          typed fetch client + one module per backend resource
  components/   presentational/data-fetching React components
  types/        TypeScript types mirroring backend model classes/DTOs
  App.tsx       page composition
```

## Current scope


The dashboard shows the service list (`GET /api/services`) with status summary
cards, colored status badges, and loading/error/empty states. Operators can
register, edit, and delete services; run checks on demand; enable or disable
monitoring; select the NORMAL or STRICT evaluation policy; inspect health-check
and status-event timelines; and acknowledge or resolve active alerts. Deleting a
service that already has monitoring history is blocked with a clear message.
