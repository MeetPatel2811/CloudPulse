import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { addIncidentNote, assignIncidentOwner, getIncident } from '../api/incidents'
import type { AlertResponse, IncidentResponse } from '../types'
import { IncidentWorkspace } from './IncidentWorkspace'

vi.mock('../api/incidents', () => ({
  getIncident: vi.fn(),
  assignIncidentOwner: vi.fn(),
  addIncidentNote: vi.fn(),
}))

const mockedGetIncident = vi.mocked(getIncident)
const mockedAssignIncidentOwner = vi.mocked(assignIncidentOwner)
const mockedAddIncidentNote = vi.mocked(addIncidentNote)

const alert: AlertResponse = {
  id: 'alert-1',
  serviceId: 'service-1',
  serviceName: 'Payments API',
  severity: 'CRITICAL',
  status: 'OPEN',
  message: 'Payments API is down',
  createdAt: '2026-08-10T12:00:00Z',
  acknowledgedAt: null,
  acknowledgedBy: null,
  resolvedAt: null,
}

const initialIncident: IncidentResponse = {
  alertId: alert.id,
  owner: 'alice',
  activity: [{
    id: 'activity-1',
    type: 'ASSIGNED',
    author: 'alice',
    detail: 'Assigned to alice',
    createdAt: '2026-08-10T12:01:00Z',
  }],
}

describe('IncidentWorkspace', () => {
  beforeEach(() => vi.resetAllMocks())

  it('loads the timeline, reassigns the owner, and adds an operator note', async () => {
    const user = userEvent.setup()
    mockedGetIncident.mockResolvedValue(initialIncident)
    mockedAssignIncidentOwner.mockResolvedValue({
      ...initialIncident,
      owner: 'bob',
      activity: [...initialIncident.activity, {
        id: 'activity-2',
        type: 'ASSIGNED',
        author: 'bob',
        detail: 'Assigned to bob',
        createdAt: '2026-08-10T12:02:00Z',
      }],
    })
    mockedAddIncidentNote.mockResolvedValue({
      ...initialIncident,
      owner: 'bob',
      activity: [...initialIncident.activity, {
        id: 'activity-3',
        type: 'NOTE',
        author: 'operator',
        detail: 'Database team is investigating.',
        createdAt: '2026-08-10T12:03:00Z',
      }],
    })

    render(<IncidentWorkspace alert={alert} onClose={vi.fn()} onSelectService={vi.fn()} />)
    const dialog = screen.getByRole('dialog', { name: 'Payments API' })

    expect(await within(dialog).findByText('Assigned to alice')).toBeInTheDocument()
    expect(mockedGetIncident).toHaveBeenCalledWith('alert-1')

    const ownerInput = within(dialog).getByLabelText('Assign or reassign owner')
    await user.clear(ownerInput)
    await user.type(ownerInput, 'bob')
    await user.click(within(dialog).getByRole('button', { name: 'Assign' }))

    await waitFor(() => expect(mockedAssignIncidentOwner).toHaveBeenCalledWith('alert-1', 'bob'))
    expect(await within(dialog).findByText('Incident assigned to bob.')).toBeInTheDocument()

    await user.type(within(dialog).getByLabelText('Timeline note'), 'Database team is investigating.')
    await user.click(within(dialog).getByRole('button', { name: 'Add note' }))

    await waitFor(() => {
      expect(mockedAddIncidentNote).toHaveBeenCalledWith(
        'alert-1',
        'Database team is investigating.',
      )
    })
    expect(await within(dialog).findByText('Database team is investigating.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Timeline note')).toHaveValue('')
  })
})
