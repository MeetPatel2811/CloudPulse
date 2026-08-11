import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getReliabilityPolicy, updateReliabilityPolicy } from '../api/reliabilityPolicy'
import type { MonitoredService } from '../types'
import { ReliabilityPolicyControl } from './ReliabilityPolicyControl'

vi.mock('../api/reliabilityPolicy', () => ({
  getReliabilityPolicy: vi.fn(),
  updateReliabilityPolicy: vi.fn(),
}))

const mockedGetReliabilityPolicy = vi.mocked(getReliabilityPolicy)
const mockedUpdateReliabilityPolicy = vi.mocked(updateReliabilityPolicy)

const monitoredService: MonitoredService = {
  id: 'service-1',
  name: 'Payments API',
  healthUrl: 'http://localhost:8081/health',
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
  lastCheckedAt: '2026-08-08T12:00:00Z',
  expectedKeyword: null,
}

describe('ReliabilityPolicyControl', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockedGetReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: null,
      effectiveLatencyThresholdMs: 1000,
      alertFailureThreshold: 1,
      consecutiveFailureCount: 0,
    })
  })

  it('loads the effective default and saves a custom threshold', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn()
    mockedUpdateReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: 750,
      effectiveLatencyThresholdMs: 750,
      alertFailureThreshold: 1,
      consecutiveFailureCount: 0,
    })

    render(<ReliabilityPolicyControl service={monitoredService} onChanged={onChanged} />)

    expect(await screen.findByText('1000 ms')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Custom' }))
    const input = screen.getByLabelText('Threshold in milliseconds')
    await user.clear(input)
    await user.type(input, '750')
    await user.click(screen.getByRole('button', { name: 'Save reliability settings' }))

    await waitFor(() => {
      expect(mockedUpdateReliabilityPolicy).toHaveBeenCalledWith('service-1', 750, 1)
    })
    expect(onChanged).toHaveBeenCalledOnce()
    expect(screen.getByText(/Reliability settings saved/)).toBeInTheDocument()
  })

  it('restores the strategy default by sending a null override', async () => {
    const user = userEvent.setup()
    mockedGetReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: 700,
      effectiveLatencyThresholdMs: 700,
      alertFailureThreshold: 1,
      consecutiveFailureCount: 2,
    })
    mockedUpdateReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: null,
      effectiveLatencyThresholdMs: 1000,
      alertFailureThreshold: 1,
      consecutiveFailureCount: 2,
    })

    render(<ReliabilityPolicyControl service={{ ...monitoredService, latencyThresholdMs: 700 }} onChanged={vi.fn()} />)

    await screen.findByText('700 ms')
    await user.click(screen.getByRole('button', { name: 'Policy default' }))
    await user.click(screen.getByRole('button', { name: 'Save reliability settings' }))

    await waitFor(() => {
      expect(mockedUpdateReliabilityPolicy).toHaveBeenCalledWith('service-1', null, 1)
    })
    expect(screen.getByText(/Reliability settings saved/)).toBeInTheDocument()
    expect(screen.getByText('1000 ms')).toBeInTheDocument()
  })

  it('rejects an out-of-range value before calling the backend', async () => {
    const user = userEvent.setup()
    render(<ReliabilityPolicyControl service={monitoredService} onChanged={vi.fn()} />)

    await screen.findByText('1000 ms')
    await user.click(screen.getByRole('button', { name: 'Custom' }))
    const input = screen.getByLabelText('Threshold in milliseconds')
    await user.clear(input)
    await user.type(input, '50')
    await user.click(screen.getByRole('button', { name: 'Save reliability settings' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Enter a whole number from 100 to 5000 ms.')
    expect(mockedUpdateReliabilityPolicy).not.toHaveBeenCalled()
  })

  it('saves the consecutive unhealthy-check trigger and shows progress', async () => {
    const user = userEvent.setup()
    mockedGetReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: null,
      effectiveLatencyThresholdMs: 1000,
      alertFailureThreshold: 3,
      consecutiveFailureCount: 1,
    })
    mockedUpdateReliabilityPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'NORMAL',
      latencyThresholdMs: null,
      effectiveLatencyThresholdMs: 1000,
      alertFailureThreshold: 4,
      consecutiveFailureCount: 1,
    })

    render(<ReliabilityPolicyControl
      service={{ ...monitoredService, alertFailureThreshold: 3, consecutiveFailureCount: 1 }}
      onChanged={vi.fn()}
    />)

    expect(await screen.findByText('1 / 3')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Alert trigger'), '4')
    await user.click(screen.getByRole('button', { name: 'Save reliability settings' }))

    await waitFor(() => {
      expect(mockedUpdateReliabilityPolicy).toHaveBeenCalledWith('service-1', null, 4)
    })
    expect(screen.getByText(/Alerts open after 4 unhealthy checks/)).toBeInTheDocument()
  })
})
