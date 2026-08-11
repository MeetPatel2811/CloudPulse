import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { probeService, registerService } from '../api/services'
import type { MonitoredService } from '../types'
import { AddMonitorWizard } from './AddMonitorWizard'

vi.mock('../api/services', () => ({
  probeService: vi.fn(),
  registerService: vi.fn(),
}))

const mockedProbeService = vi.mocked(probeService)
const mockedRegisterService = vi.mocked(registerService)

const createdService: MonitoredService = {
  id: 'notion-monitor',
  name: 'Notion',
  healthUrl: 'https://notion.com',
  serviceGroup: 'Productivity',
  tags: ['saas', 'external'],
  monitorType: 'HTTP',
  currentStatus: 'UNKNOWN',
  enabled: true,
  checkIntervalSeconds: 15,
  activeEvaluationStrategy: 'NORMAL',
  latencyThresholdMs: null,
  availabilitySloPercent: 99,
  alertFailureThreshold: 1,
  consecutiveFailureCount: 0,
  createdAt: '2026-08-09T12:00:00Z',
  lastCheckedAt: null,
  expectedKeyword: null,
}

describe('AddMonitorWizard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('normalizes, probes, and only then registers a reachable website', async () => {
    const user = userEvent.setup()
    const onCreated = vi.fn()
    const onClose = vi.fn()
    mockedProbeService.mockResolvedValue({
      reachable: true,
      httpStatus: 200,
      responseTimeMs: 182,
      detectedMode: 'GENERIC_HTTP',
      finalUrl: 'https://notion.com',
      redirectCount: 0,
      responseBodyTruncated: true,
      message: 'Connection successful',
    })
    mockedRegisterService.mockResolvedValue(createdService)

    render(<AddMonitorWizard onCreated={onCreated} onClose={onClose} />)

    const addButton = screen.getByRole('button', { name: 'Add monitor' })
    expect(addButton).toBeDisabled()

    await user.type(screen.getByLabelText('Website or health endpoint'), 'notion.com')
    await user.click(screen.getByRole('button', { name: 'Test connection' }))

    await waitFor(() => expect(mockedProbeService).toHaveBeenCalledWith('https://notion.com'))
    expect(await screen.findByText('Connection verified')).toBeInTheDocument()
    expect(screen.getByLabelText('Monitor name')).toHaveValue('Notion')
    expect(addButton).toBeEnabled()

    await user.type(screen.getByLabelText(/Service group/), ' Productivity ')
    await user.type(screen.getByLabelText(/Tags/), 'SaaS, external, saas')

    await user.click(addButton)

    await waitFor(() => {
      expect(mockedRegisterService).toHaveBeenCalledWith({
        name: 'Notion',
        healthUrl: 'https://notion.com',
        monitorType: 'HTTP',
        checkIntervalSeconds: 15,
        serviceGroup: 'Productivity',
        tags: ['saas', 'external'],
      })
    })
    expect(onCreated).toHaveBeenCalledWith(createdService)
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('keeps registration disabled when the probe cannot reach the target', async () => {
    const user = userEvent.setup()
    mockedProbeService.mockResolvedValue({
      reachable: false,
      httpStatus: 503,
      responseTimeMs: 0,
      detectedMode: 'GENERIC_HTTP',
      finalUrl: 'https://down.example.com',
      redirectCount: 0,
      responseBodyTruncated: false,
      message: 'HTTP 503',
    })

    render(<AddMonitorWizard onCreated={vi.fn()} onClose={vi.fn()} />)
    await user.type(screen.getByLabelText('Website or health endpoint'), 'down.example.com')
    await user.click(screen.getByRole('button', { name: 'Test connection' }))

    expect(await screen.findByText('Target did not pass verification')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add monitor' })).toBeDisabled()
    expect(mockedRegisterService).not.toHaveBeenCalled()
  })
})
