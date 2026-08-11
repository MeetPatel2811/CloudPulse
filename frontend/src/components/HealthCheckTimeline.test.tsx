import { render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getHealthChecks } from '../api/healthChecks'
import type { HealthCheckResultResponse } from '../types'
import { HealthCheckTimeline } from './HealthCheckTimeline'

vi.mock('../api/healthChecks', () => ({
  getHealthChecks: vi.fn(),
}))

const mockedGetHealthChecks = vi.mocked(getHealthChecks)

function result(overrides: Partial<HealthCheckResultResponse> = {}): HealthCheckResultResponse {
  return {
    id: 'check-1',
    serviceId: 'service-1',
    serviceName: 'Payments API',
    checkedAt: '2026-08-08T12:00:00Z',
    reachable: true,
    httpStatus: 200,
    responseTimeMs: 125,
    rawMessage: '{"status":"UP"}',
    evaluatedStatus: 'HEALTHY',
    ...overrides,
  }
}

describe('HealthCheckTimeline', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('renders the persisted result details returned by the history API', async () => {
    mockedGetHealthChecks.mockResolvedValue([
      result(),
      result({
        id: 'check-2',
        reachable: false,
        httpStatus: 0,
        responseTimeMs: 0,
        rawMessage: 'Connection refused',
        evaluatedStatus: 'DOWN',
      }),
    ])

    render(<HealthCheckTimeline serviceId="service-1" />)

    const timeline = await screen.findByRole('list', { name: 'Health-check history' })
    expect(mockedGetHealthChecks).toHaveBeenCalledWith('service-1')
    expect(within(timeline).getByText('Healthy')).toBeInTheDocument()
    expect(within(timeline).getByText('125 ms')).toBeInTheDocument()
    expect(within(timeline).getByText('Reachable')).toBeInTheDocument()
    expect(within(timeline).getByText('Down')).toBeInTheDocument()
    expect(within(timeline).getByText('Unreachable')).toBeInTheDocument()
    expect(within(timeline).getByText('Not available')).toBeInTheDocument()
    expect(within(timeline).getByText('No response')).toBeInTheDocument()
    expect(within(timeline).getByText('Connection refused')).toBeInTheDocument()
  })

  it('shows an empty state when the service has no recorded checks', async () => {
    mockedGetHealthChecks.mockResolvedValue([])

    render(<HealthCheckTimeline serviceId="service-1" />)

    expect(await screen.findByText('No health checks recorded yet.')).toBeInTheDocument()
  })

  it('shows a useful API error without discarding the service detail view', async () => {
    mockedGetHealthChecks.mockRejectedValue(new Error('Service not found: service-1'))

    render(<HealthCheckTimeline serviceId="service-1" />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not load health history: Service not found: service-1',
    )
  })
})
