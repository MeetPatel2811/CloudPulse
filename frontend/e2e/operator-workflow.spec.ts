import { expect, test, type Route } from '@playwright/test'

const serviceId = 'e2e-service-1'
const alertId = 'e2e-alert-1'
const serviceName = 'Checkout API'
const healthUrl = 'https://status.example.com/health'
const now = '2026-08-09T16:00:00Z'

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}

test('operator can register a monitor, run a check, see its alert, and resolve it', async ({ page }) => {
  const browserErrors: string[] = []
  page.on('pageerror', (error) => browserErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(message.text())
  })

  let services: Record<string, unknown>[] = []
  let alerts: Record<string, unknown>[] = []
  let checks: Record<string, unknown>[] = []
  let events: Record<string, unknown>[] = []
  const unexpectedRequests: string[] = []

  await page.route('http://127.0.0.1:5173/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()

    if (method === 'GET' && path === '/api/services') return json(route, services)
    if (method === 'GET' && path === '/api/alerts') return json(route, alerts)

    if (method === 'POST' && path === '/api/services/probe') {
      return json(route, {
        reachable: true,
        httpStatus: 200,
        responseTimeMs: 42,
        detectedMode: 'STANDARD_JSON',
        finalUrl: healthUrl,
        redirectCount: 0,
        responseBodyTruncated: false,
        message: 'Connection successful',
      })
    }

    if (method === 'POST' && path === '/api/services') {
      const requestBody = request.postDataJSON() as {
        name: string
        healthUrl: string
        monitorType: string
      }
      const created = {
        id: serviceId,
        ...requestBody,
        currentStatus: 'UNKNOWN',
        enabled: true,
        checkIntervalSeconds: 15,
        activeEvaluationStrategy: 'NORMAL',
        latencyThresholdMs: null,
        availabilitySloPercent: 99,
        alertFailureThreshold: 1,
        consecutiveFailureCount: 0,
        createdAt: now,
        lastCheckedAt: null,
      }
      services = [created]
      return json(route, created)
    }

    if (method === 'POST' && path === `/api/services/${serviceId}/check`) {
      const updated = {
        ...services[0],
        currentStatus: 'DOWN',
        consecutiveFailureCount: 1,
        lastCheckedAt: now,
      }
      services = [updated]
      checks = [{
        id: 'e2e-check-1',
        serviceId,
        serviceName,
        checkedAt: now,
        reachable: false,
        httpStatus: 500,
        responseTimeMs: 0,
        rawMessage: 'Simulated endpoint failure',
        evaluatedStatus: 'DOWN',
      }]
      events = [{
        id: 'e2e-event-1',
        serviceId,
        serviceName,
        previousStatus: 'UNKNOWN',
        newStatus: 'DOWN',
        reason: 'Manual health check failed',
        occurredAt: now,
      }]
      alerts = [{
        id: alertId,
        serviceId,
        serviceName,
        severity: 'CRITICAL',
        status: 'OPEN',
        message: 'Checkout API is down',
        createdAt: now,
        acknowledgedAt: null,
        acknowledgedBy: null,
        resolvedAt: null,
      }]
      return json(route, updated)
    }

    if (method === 'POST' && path === `/api/alerts/${alertId}/resolve`) {
      const resolved = { ...alerts[0], status: 'RESOLVED', resolvedAt: now }
      alerts = [resolved]
      return json(route, resolved)
    }

    if (method === 'GET' && path === `/api/services/${serviceId}/health-checks`) {
      return json(route, checks)
    }
    if (method === 'GET' && path === `/api/services/${serviceId}/status-events`) {
      return json(route, events)
    }
    if (method === 'GET' && path === `/api/services/${serviceId}/metrics`) {
      const successfulChecks = checks.filter((check) => check.reachable).length
      return json(route, {
        serviceId,
        serviceName,
        window: url.searchParams.get('window') ?? '24h',
        windowStart: now,
        windowEnd: now,
        currentStatus: services[0]?.currentStatus ?? 'UNKNOWN',
        enabled: true,
        activeEvaluationStrategy: 'NORMAL',
        totalChecks: checks.length,
        successfulChecks,
        failedChecks: checks.length - successfulChecks,
        availabilityPercent: checks.length === 0 ? 0 : (successfulChecks / checks.length) * 100,
        availabilitySloPercent: 99,
        errorBudgetRemainingPercent: checks.length === 0 ? 100 : 0,
        sloMet: checks.length === 0,
        averageResponseTimeMs: successfulChecks === 0 ? null : 42,
        p95ResponseTimeMs: successfulChecks === 0 ? null : 42,
        p95SampleSize: successfulChecks,
        p95Sampled: false,
      })
    }
    if (method === 'GET' && path === `/api/services/${serviceId}/reliability-policy`) {
      const service = services[0]
      return json(route, {
        serviceId,
        serviceName,
        evaluationPolicy: service?.activeEvaluationStrategy ?? 'NORMAL',
        latencyThresholdMs: service?.latencyThresholdMs ?? null,
        effectiveLatencyThresholdMs: service?.latencyThresholdMs ?? 1000,
        alertFailureThreshold: service?.alertFailureThreshold ?? 1,
        consecutiveFailureCount: service?.consecutiveFailureCount ?? 0,
      })
    }
    if (method === 'GET' && path === `/api/services/${serviceId}/ssl-certificate`) {
      return json(route, {
        serviceId,
        applicable: false,
        reachable: false,
        expiresAt: null,
        daysRemaining: null,
        subject: null,
        message: 'Not an HTTPS target',
        status: 'NOT_APPLICABLE',
      })
    }
    if (method === 'GET' && path === `/api/services/${serviceId}/maintenance-windows`) {
      return json(route, [])
    }

    unexpectedRequests.push(`${method} ${path}`)
    return json(route, { message: `Unexpected E2E request: ${method} ${path}` }, 500)
  })

  await page.goto('/', { waitUntil: 'networkidle' })
  expect(unexpectedRequests).toEqual([])
  expect(browserErrors).toEqual([])
  await expect(page.getByRole('heading', { name: 'Put a website or API on watch.' })).toBeVisible()

  await page.getByRole('button', { name: 'Add monitor', exact: true }).click()
  const wizard = page.getByRole('dialog', { name: 'Start watching an endpoint' })
  await expect(wizard).toBeVisible()
  await wizard.getByLabel('Website or health endpoint').fill(healthUrl)
  await wizard.getByLabel('Monitor name').fill(serviceName)
  await wizard.getByRole('button', { name: 'Test connection' }).click()
  await expect(wizard.getByText('Connection verified')).toBeVisible()
  await wizard.getByRole('button', { name: 'Add monitor', exact: true }).click()

  await expect(page.locator('.service-detail__title-row h1')).toHaveText(serviceName)
  await page.getByRole('button', { name: 'Run check now' }).click()
  await expect(page.locator('.service-detail__title-row').getByText('Down', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: /Incidents/ }).click()
  await expect(page.getByRole('heading', { name: 'Incident center' })).toBeVisible()
  const incident = page.locator('.incident-card').filter({ hasText: serviceName })
  await expect(incident.getByText('Checkout API is down')).toBeVisible()
  await incident.getByRole('button', { name: 'Resolve' }).click()

  await expect(page.getByText('No incidents match this view.')).toBeVisible()
  expect(unexpectedRequests).toEqual([])
})
