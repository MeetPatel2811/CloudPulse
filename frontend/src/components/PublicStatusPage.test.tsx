import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getPublicStatus } from '../api/publicStatus'
import { PublicStatusPage } from './PublicStatusPage'

vi.mock('../api/publicStatus', () => ({ getPublicStatus: vi.fn() }))

const mockedGetPublicStatus = vi.mocked(getPublicStatus)

describe('PublicStatusPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows grouped public service health and refreshes without exposing URLs', async () => {
    const user = userEvent.setup()
    mockedGetPublicStatus.mockResolvedValue({
      generatedAt: '2026-08-10T12:00:00Z',
      overallStatus: 'DEGRADED_PERFORMANCE',
      services: [
        { name: 'Payments API', serviceGroup: 'Revenue', status: 'HEALTHY', lastCheckedAt: '2026-08-10T12:00:00Z' },
        { name: 'Checkout', serviceGroup: 'Revenue', status: 'DEGRADED', lastCheckedAt: '2026-08-10T12:00:00Z' },
      ],
    })

    render(<PublicStatusPage />)

    expect(await screen.findByRole('heading', { name: 'Degraded performance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Revenue' })).toBeInTheDocument()
    expect(screen.getByText('Payments API')).toBeInTheDocument()
    expect(screen.getByText('Checkout')).toBeInTheDocument()
    expect(screen.getByText('Operational')).toBeInTheDocument()
    expect(screen.getByText('Degraded', { selector: 'strong' })).toBeInTheDocument()
    expect(screen.queryByText(/https?:\/\//)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Refresh' }))
    await waitFor(() => expect(mockedGetPublicStatus).toHaveBeenCalledTimes(2))
  })
})
