import type { ComponentProps } from 'react'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { deleteService, updateService } from '../api/services'
import type { MonitoredService } from '../types'
import { ServiceDirectory } from './ServiceDirectory'

vi.mock('../api/services', () => ({
  deleteService: vi.fn(),
  updateService: vi.fn(),
}))

const mockedDeleteService = vi.mocked(deleteService)
const mockedUpdateService = vi.mocked(updateService)

function service(overrides: Partial<MonitoredService> = {}): MonitoredService {
  return {
    id: 'service-1',
    name: 'Payments API',
    healthUrl: 'http://localhost:8081/health',
    serviceGroup: 'Revenue',
    tags: ['api', 'critical'],
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
    ...overrides,
  }
}

function renderDirectory(
  overrides: Partial<ComponentProps<typeof ServiceDirectory>> = {},
) {
  const props: ComponentProps<typeof ServiceDirectory> = {
    services: [service()],
    metrics: {},
    histories: {},
    onAddMonitor: vi.fn(),
    onSelectService: vi.fn(),
    onChanged: vi.fn(),
    ...overrides,
  }

  render(<ServiceDirectory {...props} />)
  return props
}

function serviceButtonFor(serviceName: string) {
  const name = screen.getByText(serviceName, { selector: 'strong' })
  const button = name.closest('button')
  if (!button) throw new Error(`Could not find the service button for ${serviceName}`)
  return button
}

function rowFor(serviceName: string) {
  const row = serviceButtonFor(serviceName).closest('tr')
  if (!row) throw new Error(`Could not find the table row for ${serviceName}`)
  return row
}

describe('ServiceDirectory', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('renders the production inventory and opens the selected service', async () => {
    const user = userEvent.setup()
    const onSelectService = vi.fn()
    renderDirectory({
      onSelectService,
      metrics: {
        'service-1': {
          serviceId: 'service-1',
          serviceName: 'Payments API',
          window: '24h',
          windowStart: '2026-08-08T12:00:00Z',
          windowEnd: '2026-08-09T12:00:00Z',
          currentStatus: 'HEALTHY',
          enabled: true,
          activeEvaluationStrategy: 'NORMAL',
          totalChecks: 100,
          successfulChecks: 99,
          failedChecks: 1,
          availabilityPercent: 99,
          availabilitySloPercent: 99,
          errorBudgetRemainingPercent: 0,
          sloMet: true,
          averageResponseTimeMs: 110,
          p95ResponseTimeMs: 180,
          p95SampleSize: 100,
          p95Sampled: false,
        },
      },
    })

    const row = rowFor('Payments API')
    expect(within(row).getByText('Healthy')).toBeInTheDocument()
    expect(within(row).getByText('99.00%')).toBeInTheDocument()
    expect(within(row).getByText('180 ms')).toBeInTheDocument()
    expect(within(row).getByText('Revenue')).toBeInTheDocument()
    expect(within(row).getByText('#critical')).toBeInTheDocument()

    await user.click(serviceButtonFor('Payments API'))
    expect(onSelectService).toHaveBeenCalledWith('service-1')
  })

  it('opens the add-monitor workflow from the production directory', async () => {
    const user = userEvent.setup()
    const onAddMonitor = vi.fn()
    renderDirectory({ onAddMonitor })

    await user.click(screen.getByRole('button', { name: 'Add monitor' }))

    expect(onAddMonitor).toHaveBeenCalledOnce()
  })

  it('shows the production empty state before any monitors are added', () => {
    renderDirectory({ services: [] })

    expect(screen.getByRole('status')).toHaveTextContent('No monitors have been added yet.')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('filters the current production inventory by name, status, group, and tag', async () => {
    const user = userEvent.setup()
    renderDirectory({
      services: [
        service(),
        service({
          id: 'service-2',
          name: 'Legacy API',
          currentStatus: 'DOWN',
          serviceGroup: 'Legacy Systems',
          tags: ['api', 'legacy'],
        }),
      ],
    })

    await user.type(screen.getByLabelText('Search monitors'), 'legacy')
    expect(screen.queryByText('Payments API')).not.toBeInTheDocument()
    expect(screen.getByText('Legacy API')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Search monitors'))
    await user.selectOptions(screen.getByLabelText('Status'), 'HEALTHY')
    expect(screen.getByText('Payments API')).toBeInTheDocument()
    expect(screen.queryByText('Legacy API')).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Status'), 'ALL')
    await user.selectOptions(screen.getByLabelText('Group'), 'Legacy Systems')
    await user.selectOptions(screen.getByLabelText('Tag'), 'legacy')
    expect(screen.queryByText('Payments API')).not.toBeInTheDocument()
    expect(screen.getByText('Legacy API')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Clear' }))
    expect(screen.getByText('Payments API')).toBeInTheDocument()
    expect(screen.getByText('Legacy API')).toBeInTheDocument()
  })

  it('shows a distinct empty state when active filters have no matches', async () => {
    const user = userEvent.setup()
    renderDirectory()

    await user.type(screen.getByLabelText('Search monitors'), 'catalog')

    expect(screen.getByRole('status')).toHaveTextContent('No monitors match these filters.')
    expect(screen.queryByText('Payments API')).not.toBeInTheDocument()
  })

  it('edits a monitor through the production form and refreshes the dashboard', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn().mockResolvedValue(undefined)
    mockedUpdateService.mockResolvedValue(service({ name: 'Payments Platform', monitorType: 'MOCK' }))
    renderDirectory({ onChanged })

    await user.click(within(rowFor('Payments API')).getByRole('button', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit monitor' })
    const nameInput = within(dialog).getByLabelText('Name')
    await user.clear(nameInput)
    await user.type(nameInput, 'Payments Platform')
    await user.selectOptions(within(dialog).getByLabelText('Monitor type'), 'MOCK')
    const groupInput = within(dialog).getByLabelText(/Service group/)
    await user.clear(groupInput)
    await user.type(groupInput, 'Core Platform')
    const tagsInput = within(dialog).getByLabelText(/Tags/)
    await user.clear(tagsInput)
    await user.type(tagsInput, 'API, tier-1, api')
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    await waitFor(() => {
      expect(mockedUpdateService).toHaveBeenCalledWith('service-1', {
        name: 'Payments Platform',
        healthUrl: 'http://localhost:8081/health',
        monitorType: 'MOCK',
        checkIntervalSeconds: 15,
        serviceGroup: 'Core Platform',
        tags: ['api', 'tier-1'],
      })
    })
    await waitFor(() => expect(onChanged).toHaveBeenCalledOnce())
    expect(screen.queryByRole('dialog', { name: 'Edit monitor' })).not.toBeInTheDocument()
  })

  it('keeps the edit form open and shows the backend error when updating fails', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn()
    mockedUpdateService.mockRejectedValue(new Error('A monitor with that URL already exists'))
    renderDirectory({ onChanged })

    await user.click(within(rowFor('Payments API')).getByRole('button', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit monitor' })
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      'A monitor with that URL already exists',
    )
    expect(onChanged).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog', { name: 'Edit monitor' })).toBeInTheDocument()
  })

  it('deletes a monitor after confirmation and refreshes the dashboard', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn().mockResolvedValue(undefined)
    mockedDeleteService.mockResolvedValue()
    renderDirectory({ onChanged })

    await user.click(within(rowFor('Payments API')).getByRole('button', { name: 'Delete' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Delete monitor' })
    expect(within(dialog).getByText(/Delete “Payments API”/)).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Delete monitor' }))

    await waitFor(() => expect(mockedDeleteService).toHaveBeenCalledWith('service-1'))
    await waitFor(() => expect(onChanged).toHaveBeenCalledOnce())
    expect(screen.queryByRole('alertdialog', { name: 'Delete monitor' })).not.toBeInTheDocument()
  })

  it('closes the confirmation and displays the backend message when deletion fails', async () => {
    const user = userEvent.setup()
    const onChanged = vi.fn()
    mockedDeleteService.mockRejectedValue(
      new Error('Cannot delete a monitor with recorded history. Disable it instead.'),
    )
    renderDirectory({ onChanged })

    await user.click(within(rowFor('Payments API')).getByRole('button', { name: 'Delete' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Delete monitor' })
    await user.click(within(dialog).getByRole('button', { name: 'Delete monitor' }))

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent('Cannot delete a monitor with recorded history. Disable it instead.')
    expect(onChanged).not.toHaveBeenCalled()
    expect(screen.queryByRole('alertdialog', { name: 'Delete monitor' })).not.toBeInTheDocument()
  })
})
