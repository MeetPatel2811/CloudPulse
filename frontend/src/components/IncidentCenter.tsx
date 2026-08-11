import { useMemo, useState } from 'react'
import { acknowledgeAlert, resolveAlert } from '../api/alerts'
import type { AlertResponse, AlertStatus } from '../types'
import { formatDateTime } from '../lib/format'
import { Icon } from './Icon'
import { IncidentWorkspace } from './IncidentWorkspace'
import { NotificationCenter } from './NotificationCenter'

type IncidentFilter = 'ACTIVE' | AlertStatus | 'ALL'
type IncidentSection = 'INCIDENTS' | 'NOTIFICATIONS'

export function IncidentCenter({
  alerts,
  onChanged,
  onSelectService,
}: {
  alerts: AlertResponse[]
  onChanged: () => void | Promise<void>
  onSelectService: (serviceId: string) => void
}) {
  const [filter, setFilter] = useState<IncidentFilter>('ACTIVE')
  const [section, setSection] = useState<IncidentSection>('INCIDENTS')
  const [selectedIncident, setSelectedIncident] = useState<AlertResponse | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const visible = useMemo(() => alerts.filter((alert) => {
    if (filter === 'ALL') return true
    if (filter === 'ACTIVE') return alert.status !== 'RESOLVED'
    return alert.status === filter
  }), [alerts, filter])

  const counts = {
    open: alerts.filter((alert) => alert.status === 'OPEN').length,
    acknowledged: alerts.filter((alert) => alert.status === 'ACKNOWLEDGED').length,
    resolved: alerts.filter((alert) => alert.status === 'RESOLVED').length,
  }

  async function run(id: string, action: () => Promise<unknown>) {
    setBusyId(id)
    setError(null)
    try {
      await action()
      await onChanged()
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'The incident action could not be completed')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="incident-center">
      {selectedIncident && (
        <IncidentWorkspace
          alert={selectedIncident}
          onClose={() => setSelectedIncident(null)}
          onSelectService={(serviceId) => {
            setSelectedIncident(null)
            onSelectService(serviceId)
          }}
        />
      )}

      <header className="view-header">
        <div>
          <p className="eyebrow">Response desk</p>
          <h1>Incident center</h1>
          <p>Coordinate incident ownership, response notes, and outbound alert delivery.</p>
        </div>
      </header>

      <div className="incident-section-tabs" role="tablist" aria-label="Incident center sections">
        <button
          type="button"
          role="tab"
          aria-selected={section === 'INCIDENTS'}
          className={section === 'INCIDENTS' ? 'incident-section-tabs__active' : ''}
          onClick={() => setSection('INCIDENTS')}
        >
          {section === 'NOTIFICATIONS' && <Icon name="arrow-left" className="h-3.5 w-3.5" />}
          Incidents <span>{counts.open + counts.acknowledged}</span>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={section === 'NOTIFICATIONS'}
          className={section === 'NOTIFICATIONS' ? 'incident-section-tabs__active' : ''}
          onClick={() => setSection('NOTIFICATIONS')}
        >
          Notifications
        </button>
      </div>

      {section === 'NOTIFICATIONS' ? (
        <NotificationCenter />
      ) : (
        <>

      <section className="incident-summary" aria-label="Incident status summary">
        <article><span className="incident-dot incident-dot--critical" /><div><strong>{counts.open}</strong><p>Open</p></div></article>
        <article><span className="incident-dot incident-dot--warning" /><div><strong>{counts.acknowledged}</strong><p>Acknowledged</p></div></article>
        <article><span className="incident-dot incident-dot--resolved" /><div><strong>{counts.resolved}</strong><p>Resolved</p></div></article>
      </section>

      <div className="incident-toolbar">
        <div className="segmented-control" aria-label="Filter incidents">
          {(['ACTIVE', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'ALL'] as IncidentFilter[]).map((value) => (
            <button
              type="button"
              key={value}
              onClick={() => setFilter(value)}
              aria-pressed={filter === value}
              className={filter === value ? 'segmented-control__active' : ''}
            >
              {value.charAt(0) + value.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <span>{visible.length} {visible.length === 1 ? 'incident' : 'incidents'}</span>
      </div>

      {error && <div role="alert" className="page-error">{error}</div>}

      {visible.length === 0 ? (
        <div className="quiet-state quiet-state--large" role="status">
          <Icon name="check" className="h-5 w-5" />
          <span>No incidents match this view.</span>
        </div>
      ) : (
        <div className="incident-list">
          {visible.map((alert) => {
            const busy = alert.id === busyId
            return (
              <article key={alert.id} className={`incident-card incident-card--${alert.severity.toLowerCase()}`} aria-busy={busy}>
                <div className="incident-card__severity">
                  <span className={`incident-dot incident-dot--${alert.severity.toLowerCase()}`} />
                  {alert.severity.charAt(0) + alert.severity.slice(1).toLowerCase()}
                </div>
                <div className="incident-card__body">
                  <div className="incident-card__title">
                    <div>
                      <h2>{alert.serviceName ?? 'Unknown service'}</h2>
                      <p>{alert.message}</p>
                    </div>
                    <span className={`incident-status incident-status--${alert.status.toLowerCase()}`}>
                      {alert.status.toLowerCase()}
                    </span>
                  </div>
                  <dl>
                    <div><dt>Raised</dt><dd>{formatDateTime(alert.createdAt)}</dd></div>
                    <div><dt>Acknowledged by</dt><dd>{alert.acknowledgedBy ?? 'Nobody yet'}</dd></div>
                    <div><dt>Severity</dt><dd>{alert.severity}</dd></div>
                  </dl>
                </div>
                <div className="incident-card__actions">
                  <button type="button" className="button button--secondary" onClick={() => setSelectedIncident(alert)}>
                    Manage incident
                  </button>
                  {alert.serviceId && (
                    <button type="button" className="button button--secondary" onClick={() => onSelectService(alert.serviceId!)}>
                      Inspect service
                    </button>
                  )}
                  {alert.status === 'OPEN' && (
                    <button
                      type="button"
                      className="button button--secondary"
                      disabled={busy}
                      onClick={() => run(alert.id, () => acknowledgeAlert(alert.id, 'operator'))}
                    >
                      Acknowledge
                    </button>
                  )}
                  {alert.status !== 'RESOLVED' && (
                    <button
                      type="button"
                      className="button button--primary"
                      disabled={busy}
                      onClick={() => run(alert.id, () => resolveAlert(alert.id))}
                    >
                      Resolve
                    </button>
                  )}
                </div>
              </article>
            )
          })}
        </div>
      )}
        </>
      )}
    </div>
  )
}
