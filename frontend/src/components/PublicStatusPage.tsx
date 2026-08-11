import { useCallback, useEffect, useMemo, useState } from 'react'
import { getPublicStatus } from '../api/publicStatus'
import type { PublicOverallStatus, PublicStatusResponse, ServiceStatus } from '../types'
import { formatDateTime } from '../lib/format'
import { Icon } from './Icon'

const OVERALL_COPY: Record<PublicOverallStatus, { title: string; message: string; tone: string }> = {
  OPERATIONAL: { title: 'All systems operational', message: 'Every monitored CloudPulse service is responding normally.', tone: 'healthy' },
  DEGRADED_PERFORMANCE: { title: 'Degraded performance', message: 'One or more services are responding more slowly than expected.', tone: 'warning' },
  MAJOR_OUTAGE: { title: 'Service disruption', message: 'One or more services are currently unavailable. Our team is investigating.', tone: 'incident' },
  CHECKING: { title: 'Status checks starting', message: 'CloudPulse is collecting the first health signals.', tone: 'checking' },
  NO_SERVICES: { title: 'No published services', message: 'There are currently no services included on this status page.', tone: 'checking' },
}

function publicStatusLabel(status: ServiceStatus): string {
  if (status === 'HEALTHY' || status === 'RECOVERED') return 'Operational'
  if (status === 'DEGRADED') return 'Degraded'
  if (status === 'DOWN') return 'Outage'
  return 'Checking'
}

function publicStatusTone(status: ServiceStatus): string {
  if (status === 'HEALTHY' || status === 'RECOVERED') return 'healthy'
  if (status === 'DEGRADED') return 'warning'
  if (status === 'DOWN') return 'incident'
  return 'checking'
}

export function PublicStatusPage() {
  const [status, setStatus] = useState<PublicStatusResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      const nextStatus = await getPublicStatus()
      setStatus(nextStatus)
      setError(null)
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Status information is temporarily unavailable')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
    const timer = window.setInterval(() => void refresh(), 30_000)
    return () => window.clearInterval(timer)
  }, [refresh])

  const groups = useMemo(() => {
    const grouped = new Map<string, PublicStatusResponse['services']>()
    for (const service of status?.services ?? []) {
      const group = service.serviceGroup ?? 'Other services'
      grouped.set(group, [...(grouped.get(group) ?? []), service])
    }
    return Array.from(grouped.entries())
  }, [status])

  const overall = status ? OVERALL_COPY[status.overallStatus] : OVERALL_COPY.CHECKING

  return (
    <div className="public-status-shell">
      <header className="public-status-header">
        <a href="/status" className="public-status-brand" aria-label="CloudPulse status home">
          <span className="brand-mark" aria-hidden="true"><span /><span /><span /></span>
          <span><strong>CloudPulse</strong><small>System status</small></span>
        </a>
        <button type="button" className="public-refresh" onClick={() => void refresh()} disabled={loading}>
          <Icon name="refresh" className={loading ? 'spin' : ''} /> Refresh
        </button>
      </header>

      <main className="public-status-main">
        <section className={`public-status-hero public-status-hero--${overall.tone}`} aria-live="polite">
          <span className="public-status-hero__icon"><Icon name={overall.tone === 'incident' ? 'incidents' : 'check'} /></span>
          <div><p>Current status</p><h1>{loading && !status ? 'Checking system status…' : overall.title}</h1><span>{overall.message}</span></div>
        </section>

        {error && (
          <div className="public-status-error" role="alert">
            <span>Live status could not be refreshed. {error}</span>
            <button type="button" onClick={() => void refresh()}>Try again</button>
          </div>
        )}

        <section className="public-status-services" aria-labelledby="published-services-title">
          <div className="public-status-section-heading">
            <div><p>Live components</p><h2 id="published-services-title">Service health</h2></div>
            {status && <time dateTime={status.generatedAt}>Updated {formatDateTime(status.generatedAt)}</time>}
          </div>

          {loading && !status ? (
            <div className="public-status-loading" role="status">Loading published services…</div>
          ) : groups.length === 0 ? (
            <div className="public-status-loading">No services are currently published.</div>
          ) : groups.map(([group, services]) => (
            <section key={group} className="public-status-group" aria-labelledby={`status-group-${group.replace(/\W+/g, '-').toLowerCase()}`}>
              <h3 id={`status-group-${group.replace(/\W+/g, '-').toLowerCase()}`}>{group}</h3>
              <div>
                {services.map((service) => (
                  <article key={service.name}>
                    <span>{service.name}</span>
                    <strong className={`public-component-status public-component-status--${publicStatusTone(service.status)}`}>
                      <i aria-hidden="true" />{publicStatusLabel(service.status)}
                    </strong>
                  </article>
                ))}
              </div>
            </section>
          ))}
        </section>
      </main>

      <footer className="public-status-footer">
        <span>Powered by CloudPulse monitoring</span>
        <span>Automatic updates every 30 seconds</span>
      </footer>
    </div>
  )
}

