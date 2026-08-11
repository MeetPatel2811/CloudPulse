import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  cancelMaintenanceWindow,
  getMaintenanceWindows,
  scheduleMaintenanceWindow,
} from '../api/maintenanceWindows'
import type { MaintenanceWindowResponse, MonitoredService } from '../types'
import { MaintenanceWindowControl } from './MaintenanceWindowControl'

vi.mock('../api/maintenanceWindows', () => ({
  getMaintenanceWindows: vi.fn(),
  scheduleMaintenanceWindow: vi.fn(),
  cancelMaintenanceWindow: vi.fn(),
}))

const mockedGet = vi.mocked(getMaintenanceWindows)
const mockedSchedule = vi.mocked(scheduleMaintenanceWindow)
const mockedCancel = vi.mocked(cancelMaintenanceWindow)

const monitoredService: MonitoredService = {
  id: 'service-1',
  name: 'Payments API',
  healthUrl: 'http://localhost:8081/health',
  serviceGroup: null,
  tags: [],
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

const activeWindow: MaintenanceWindowResponse = {
  id: 'window-1',
  serviceId: 'service-1',
  serviceName: 'Payments API',
  startsAt: '2026-08-09T00:00:00Z',
  endsAt: '2026-08-09T04:00:00Z',
  reason: 'Database migration',
  status: 'ACTIVE',
}

describe('MaintenanceWindowControl', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists existing windows and reports an active one to the parent', async () => {
    mockedGet.mockResolvedValue([activeWindow])
    const onActiveWindowChange = vi.fn()

    render(
      <MaintenanceWindowControl
        service={monitoredService}
        onActiveWindowChange={onActiveWindowChange}
      />,
    )

    expect(await screen.findByText('Database migration')).toBeInTheDocument()
    expect(screen.getByText('Maintenance active')).toBeInTheDocument()
    await waitFor(() => expect(onActiveWindowChange).toHaveBeenCalledWith(true))
  })

  it('schedules a new window and refreshes the list', async () => {
    const user = userEvent.setup()
    mockedGet.mockResolvedValueOnce([]).mockResolvedValueOnce([activeWindow])
    mockedSchedule.mockResolvedValue(activeWindow)
    const onChanged = vi.fn()

    render(<MaintenanceWindowControl service={monitoredService} onChanged={onChanged} />)
    await waitFor(() => expect(mockedGet).toHaveBeenCalledTimes(1))

    await user.type(screen.getByLabelText('Starts'), '2026-08-09T00:00')
    await user.type(screen.getByLabelText('Ends'), '2026-08-09T04:00')
    await user.type(screen.getByLabelText('Reason (optional)'), 'Database migration')
    await user.click(screen.getByRole('button', { name: 'Schedule window' }))

    await waitFor(() => expect(mockedSchedule).toHaveBeenCalledTimes(1))
    expect(mockedSchedule.mock.calls[0][0]).toBe('service-1')
    expect(mockedSchedule.mock.calls[0][1].reason).toBe('Database migration')
    await waitFor(() => expect(onChanged).toHaveBeenCalledOnce())
    expect(await screen.findByText('Database migration')).toBeInTheDocument()
  })

  it('cancels a window', async () => {
    const user = userEvent.setup()
    mockedGet.mockResolvedValueOnce([activeWindow]).mockResolvedValueOnce([])
    mockedCancel.mockResolvedValue(undefined)

    render(<MaintenanceWindowControl service={monitoredService} />)
    await screen.findByText('Database migration')

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    await waitFor(() => expect(mockedCancel).toHaveBeenCalledWith('service-1', 'window-1'))
    await waitFor(() => expect(screen.queryByText('Database migration')).not.toBeInTheDocument())
  })

  it('shows the backend error when scheduling fails', async () => {
    const user = userEvent.setup()
    mockedGet.mockResolvedValue([])
    mockedSchedule.mockRejectedValue(new Error('endsAt must be after startsAt'))

    render(<MaintenanceWindowControl service={monitoredService} />)
    await waitFor(() => expect(mockedGet).toHaveBeenCalledTimes(1))

    await user.type(screen.getByLabelText('Starts'), '2026-08-09T04:00')
    await user.type(screen.getByLabelText('Ends'), '2026-08-09T00:00')
    await user.click(screen.getByRole('button', { name: 'Schedule window' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('endsAt must be after startsAt')
  })
})
