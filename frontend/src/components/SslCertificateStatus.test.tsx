import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getSslCertificate } from '../api/sslCertificate'
import type { MonitoredService, SslCertificateResponse } from '../types'
import { SslCertificateStatus } from './SslCertificateStatus'

vi.mock('../api/sslCertificate', () => ({
  getSslCertificate: vi.fn(),
}))

const mockedGetSslCertificate = vi.mocked(getSslCertificate)

const monitoredService: MonitoredService = {
  id: 'service-1',
  name: 'Payments API',
  healthUrl: 'https://payments.example/health',
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

function response(overrides: Partial<SslCertificateResponse>): SslCertificateResponse {
  return {
    serviceId: 'service-1',
    applicable: true,
    reachable: true,
    expiresAt: '2026-12-01T00:00:00Z',
    daysRemaining: 90,
    subject: 'CN=payments.example',
    message: 'Certificate retrieved',
    status: 'VALID',
    ...overrides,
  }
}

describe('SslCertificateStatus', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('renders nothing for an HTTP-only service', async () => {
    mockedGetSslCertificate.mockResolvedValue(
      response({ applicable: false, reachable: false, expiresAt: null, daysRemaining: null, subject: null, status: 'NOT_APPLICABLE', message: 'Not an HTTPS target' }),
    )

    const { container } = render(<SslCertificateStatus service={monitoredService} />)

    await waitFor(() => expect(mockedGetSslCertificate).toHaveBeenCalledWith('service-1'))
    expect(container).toBeEmptyDOMElement()
  })

  it('shows days remaining for a valid certificate', async () => {
    mockedGetSslCertificate.mockResolvedValue(response({}))

    render(<SslCertificateStatus service={monitoredService} />)

    expect(await screen.findByText(/Expires in 90 days/)).toBeInTheDocument()
  })

  it('flags an expired certificate', async () => {
    mockedGetSslCertificate.mockResolvedValue(
      response({ status: 'EXPIRED', daysRemaining: -3, expiresAt: '2026-08-01T00:00:00Z' }),
    )

    render(<SslCertificateStatus service={monitoredService} />)

    expect(await screen.findByText(/Expired/)).toBeInTheDocument()
  })

  it('re-checks when Recheck is clicked', async () => {
    const user = userEvent.setup()
    mockedGetSslCertificate
      .mockResolvedValueOnce(response({ daysRemaining: 90 }))
      .mockResolvedValueOnce(response({ daysRemaining: 89 }))

    render(<SslCertificateStatus service={monitoredService} />)
    await screen.findByText(/Expires in 90 days/)

    await user.click(screen.getByRole('button', { name: 'Recheck' }))

    expect(await screen.findByText(/Expires in 89 days/)).toBeInTheDocument()
    expect(mockedGetSslCertificate).toHaveBeenCalledTimes(2)
  })

  it('shows an error when the check itself fails', async () => {
    mockedGetSslCertificate.mockRejectedValue(new Error('Service not found: service-1'))

    render(<SslCertificateStatus service={monitoredService} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Service not found: service-1')
  })
})
