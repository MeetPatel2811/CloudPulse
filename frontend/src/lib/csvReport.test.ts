import { describe, expect, it } from 'vitest'
import type { MonitoredService, ServiceMetricsResponse } from '../types'
import { buildSloCsv } from './csvReport'

const service: MonitoredService = {
  id: 'service-1',
  name: 'Payments, API',
  healthUrl: 'https://payments.example.com/health',
  serviceGroup: 'Revenue',
  tags: ['api', 'tier-1'],
  monitorType: 'HTTP',
  currentStatus: 'HEALTHY',
  enabled: true,
  checkIntervalSeconds: 15,
  activeEvaluationStrategy: 'NORMAL',
  latencyThresholdMs: null,
  availabilitySloPercent: 99,
  alertFailureThreshold: 1,
  consecutiveFailureCount: 0,
  createdAt: '2026-08-01T12:00:00Z',
  lastCheckedAt: '2026-08-10T12:00:00Z',
  expectedKeyword: null,
}

const metric: ServiceMetricsResponse = {
  serviceId: service.id,
  serviceName: service.name,
  window: '24h',
  windowStart: '2026-08-09T12:00:00Z',
  windowEnd: '2026-08-10T12:00:00Z',
  currentStatus: 'HEALTHY',
  enabled: true,
  activeEvaluationStrategy: 'NORMAL',
  totalChecks: 100,
  successfulChecks: 100,
  failedChecks: 0,
  availabilityPercent: 100,
  availabilitySloPercent: 99,
  errorBudgetRemainingPercent: 100,
  sloMet: true,
  averageResponseTimeMs: 90,
  p95ResponseTimeMs: 130,
  p95SampleSize: 100,
  p95Sampled: false,
}

describe('buildSloCsv', () => {
  it('exports the selected window and escapes report fields safely', () => {
    const csv = buildSloCsv([service], { [service.id]: metric }, '24h')

    expect(csv).toContain('"Service","Group","Tags"')
    expect(csv).toContain('"Payments, API","Revenue","api; tier-1"')
    expect(csv).toContain('"24h","99","100","100","0","100","100","MET"')
    expect(csv).not.toContain(service.healthUrl)
  })
})
