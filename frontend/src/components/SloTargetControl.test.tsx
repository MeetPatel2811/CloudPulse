import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { updateSloTarget } from '../api/slo'
import type { MonitoredService } from '../types'
import { SloTargetControl } from './SloTargetControl'

vi.mock('../api/slo', () => ({ updateSloTarget: vi.fn() }))

const mockedUpdateSloTarget = vi.mocked(updateSloTarget)
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

describe('SloTargetControl', () => {
  beforeEach(() => vi.resetAllMocks())

  it('updates the availability objective and announces success', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn()
    mockedUpdateSloTarget.mockResolvedValue({ ...service, availabilitySloPercent: 99.9 })
    render(<SloTargetControl service={service} onChanged={onChanged} />)

    await user.selectOptions(screen.getByLabelText('Target'), '99.9')
    await user.click(screen.getByRole('button', { name: 'Save SLO' }))

    await waitFor(() => expect(mockedUpdateSloTarget).toHaveBeenCalledWith('service-1', 99.9))
    expect(await screen.findByRole('status')).toHaveTextContent('99.9% availability objective saved.')
    expect(onChanged).toHaveBeenCalledOnce()
  })
})
