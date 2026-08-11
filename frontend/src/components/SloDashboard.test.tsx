import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { MonitoredService, ServiceMetricsResponse } from '../types'
import { SloDashboard } from './SloDashboard'

const service: MonitoredService = {
  id: 'service-1',
  name: 'Payments API',
  healthUrl: 'https://payments.example.com/health',
  serviceGroup: 'Revenue',
  tags: ['api'],
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
  averageResponseTimeMs: 100,
  p95ResponseTimeMs: 150,
  p95SampleSize: 100,
  p95Sampled: false,
}

describe('SloDashboard', () => {
  it('reports fleet objectives and lets the operator change the reporting window', async () => {
    const user = userEvent.setup()
    const onWindowChange = vi.fn()
    const onSelectService = vi.fn()

    render(
      <SloDashboard
        services={[service]}
        metrics={{ [service.id]: metric }}
        window="24h"
        loading={false}
        onWindowChange={onWindowChange}
        onSelectService={onSelectService}
      />,
    )

    expect(screen.getByText('1/1')).toBeInTheDocument()
    const row = screen.getByText('Payments API').closest('tr')
    if (!row) throw new Error('SLO row not found')
    expect(within(row).getByText('100.00%')).toBeInTheDocument()
    expect(within(row).getByText('On target')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '7d' }))
    expect(onWindowChange).toHaveBeenCalledWith('7d')
    await user.click(within(row).getByRole('button', { name: /Payments API/ }))
    expect(onSelectService).toHaveBeenCalledWith('service-1')
  })
})
