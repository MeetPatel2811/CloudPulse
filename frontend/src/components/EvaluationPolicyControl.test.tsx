import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { updateEvaluationPolicy } from '../api/evaluationPolicy'
import type { MonitoredService } from '../types'
import { EvaluationPolicyControl } from './EvaluationPolicyControl'

vi.mock('../api/evaluationPolicy', () => ({
  updateEvaluationPolicy: vi.fn(),
}))

const mockedUpdateEvaluationPolicy = vi.mocked(updateEvaluationPolicy)

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

describe('EvaluationPolicyControl', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('updates the selected evaluation policy and refreshes service data', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn()
    mockedUpdateEvaluationPolicy.mockResolvedValue({
      serviceId: monitoredService.id,
      serviceName: monitoredService.name,
      evaluationPolicy: 'STRICT',
      currentStatus: 'HEALTHY',
    })

    render(<EvaluationPolicyControl service={monitoredService} onChanged={onChanged} />)

    await user.selectOptions(screen.getByLabelText('Evaluation policy'), 'STRICT')

    await waitFor(() => {
      expect(mockedUpdateEvaluationPolicy).toHaveBeenCalledWith('service-1', 'STRICT')
    })
    expect(onChanged).toHaveBeenCalledOnce()
    expect(screen.getByLabelText('Evaluation policy')).toHaveValue('STRICT')
    expect(screen.getByText(/STRICT policy saved/)).toBeInTheDocument()
  })

  it('restores the previous policy and shows the backend error when an update fails', async () => {
    const user = userEvent.setup()
    mockedUpdateEvaluationPolicy.mockRejectedValue(new Error('Service not found: service-1'))

    render(<EvaluationPolicyControl service={monitoredService} onChanged={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Evaluation policy'), 'STRICT')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not update policy: Service not found: service-1',
    )
    expect(screen.getByLabelText('Evaluation policy')).toHaveValue('NORMAL')
  })
})
