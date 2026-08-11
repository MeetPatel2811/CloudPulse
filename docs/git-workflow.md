# Contributing to CloudPulse

This document defines how we branch, commit, review, and merge code for this project.
Everyone (Katha, Ashwin, Meet, Pavithra) should follow this so `main` stays stable and
demoable at all times.

## Branching

- `main` is protected — it should always build and pass tests. Nobody pushes to it directly.
- Create a feature branch off `main` for every piece of work, using this pattern:

  ```
  <initials>/<type>/<short-description>
  ```

  Where `<type>` is one of `feature`, `chore`, or `bugfix`.

  Examples:
  - `kp/feature/http-monitor`
  - `kp/feature/monitor-decorators`
  - `kp/chore/setup-ci`
  - `at/feature/state-pattern`
  - `mp/feature/alert-chain`
  - `pp/feature/dashboard-ui`
  - `mp/bugfix/null-alert-severity`

- Keep branches small and short-lived. One branch per pattern/feature, not one big branch per person for the whole milestone.
- Delete the branch after it's merged.

## Commits

- Write commits in, small and descriptive: `Add RetryMonitorDecorator with backoff`.
- It's fine to commit often on your own branch; PRs get squash-merged so history on `main` stays clean.

## Pull Requests

- Open a PR from your feature branch into `main` as soon as the piece is working, even if small — don't batch up huge PRs.
- PR description should say: what pattern/feature this implements, and how to verify it (e.g. which test to run, or what to click in the demo).
- **No mandatory reviewer approval** — with a 4-person team on a deadline, we don't want merges blocked on someone else being available. You can merge your own PR once it's ready.
- That said, if your change touches a shared entity (`MonitoredService`, `HealthCheckResult`, `Alert`, `StatusEvent`) or another package's interface, **ping the relevant teammate before merging** so nobody's surprised — see the table below for who owns what.
- **CI must pass** (build + tests) before merge is allowed, once status checks are enabled (see below).
- Use **Squash and merge** — keeps `main` history to one commit per feature.
- If your change touches a shared model/entity that another package depends on, ping the relevant teammate in the PR before merging:
  - `model/` changes → affects everyone
  - `HealthCheckResult` shape → affects monitor/, adapter/, evaluation/, handler/
  - `ServiceStatus` transitions → affects state/, handler/, observer/

## Branch Protection Rules (set on GitHub, Settings → Branches)

Currently enabled on `main`:
- Require a pull request before merging to `main`
- Do not allow bypassing the above, even for admins

Not yet enabled, to be turned on once real backend/frontend code exists and CI has passed at least once:
- Require status checks (`build-and-test`, `build`) to pass before merging

We are **not** requiring reviewer approvals — see PR section above for why.

## CI

- Every PR and every push to `main` triggers GitHub Actions:
  - `backend-ci.yml` — runs `mvn clean verify` (build + unit tests) for the backend
  - `frontend-ci.yml` — runs `npm ci && npm run build` for the React app
- A red CI check on your PR means merging is blocked until it's fixed — don't merge around it.
