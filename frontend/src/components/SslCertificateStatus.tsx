import { useEffect, useState } from 'react'
import { getSslCertificate } from '../api/sslCertificate'
import type { MonitoredService, SslCertificateResponse } from '../types'
import { formatDateTime } from '../lib/format'

const TONE_CLASSES: Record<SslCertificateResponse['status'], string> = {
  NOT_APPLICABLE: 'text-gray-500',
  UNREACHABLE: 'text-gray-500',
  VALID: 'text-green-700',
  EXPIRING_SOON: 'text-amber-700',
  EXPIRED: 'text-red-700',
}

function describe(certificate: SslCertificateResponse): string {
  if (certificate.status === 'VALID' || certificate.status === 'EXPIRING_SOON') {
    const days = certificate.daysRemaining ?? 0
    const dayLabel = days === 1 ? '1 day' : `${days} days`
    return `Expires in ${dayLabel} (${formatDateTime(certificate.expiresAt)})`
  }
  if (certificate.status === 'EXPIRED') {
    return `Expired ${formatDateTime(certificate.expiresAt)}`
  }
  return certificate.message
}

/** Renders nothing for HTTP-only services; there is no certificate to report on. */
export function SslCertificateStatus({ service }: { service: MonitoredService }) {
  const [certificate, setCertificate] = useState<SslCertificateResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [checking, setChecking] = useState(false)

  async function check() {
    setChecking(true)
    setError(null)
    try {
      setCertificate(await getSslCertificate(service.id))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Could not check the certificate')
    } finally {
      setChecking(false)
    }
  }

  useEffect(() => {
    void check()
  }, [service.id])

  if (!error && certificate?.status === 'NOT_APPLICABLE') {
    return null
  }

  return (
    <div className="rounded-md border border-gray-200 bg-gray-50 p-3">
      <div className="flex items-center justify-between gap-2">
        <div>
          <p className="text-sm font-medium text-gray-800">SSL certificate</p>
          <p className={`text-xs ${certificate ? TONE_CLASSES[certificate.status] : 'text-gray-500'}`}>
            {checking && !certificate ? 'Checking…' : certificate ? describe(certificate) : ''}
          </p>
        </div>
        <button
          type="button"
          disabled={checking}
          onClick={() => void check()}
          className="rounded-md border border-gray-300 px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
        >
          {checking ? 'Checking…' : 'Recheck'}
        </button>
      </div>
      {error && <p role="alert" className="mt-2 text-xs text-red-700">{error}</p>}
    </div>
  )
}
