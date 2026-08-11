import { lazy, Suspense, useEffect, useState } from 'react'
import { getHealthChecks } from '../api/healthChecks'
import { getServiceMetrics } from '../api/metrics'
import type {
  AlertResponse,
  HealthCheckResultResponse,
  MetricsWindow,
  MonitoredService,
  ServiceMetricsResponse,
} from '../types'
import { formatDateTime, formatLatency } from '../lib/format'
import { EvaluationPolicyControl } from './EvaluationPolicyControl'
import { HealthCheckTimeline } from './HealthCheckTimeline'
import { Icon } from './Icon'
import { MaintenanceWindowControl } from './MaintenanceWindowControl'
import { PulseRail } from './PulseRail'
import { ReliabilityPolicyControl } from './ReliabilityPolicyControl'
import { ServiceControls } from './ServiceControls'
import { SloTargetControl } from './SloTargetControl'
import { SslCertificateStatus } from './SslCertificateStatus'
import { StatusBadge } from './StatusBadge'
import { StatusTimeline } from './StatusTimeline'

const WINDOWS: MetricsWindow[] = ['1h', '6h', '24h', '7d']
const ResponseTimeChart = lazy(() =>
  import('./ResponseTimeChart').then((module) => ({ default: module.ResponseTimeChart })),
)
const WINDOW_MILLISECONDS: Record<MetricsWindow, number> = {
  '1h': 3_600_000,
  '6h': 21_600_000,
  '24h': 86_400_000,
  '7d': 604_800_000,
}

export function ServiceDetailView({
  service,
  alerts,
  refreshVersion,
  onBack,
  onChanged,
}: {
  service: MonitoredService
  alerts: AlertResponse[]
  refreshVersion: number
  onBack: () => void
  onChanged: () => void | Promise<void>
}) {
  const [window, setWindow] = useState<MetricsWindow>('24h')
  const [metrics, setMetrics] = useState<ServiceMetricsResponse | null>(null)
  const [history, setHistory] = useState<HealthCheckResultResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [localVersion, setLocalVersion] = useState(0)
  const [underMaintenance, setUnderMaintenance] = useState(false)

  useEffect(() => {
    let cancelled = false
    setError(null)
    const from = new Date(Date.now() - WINDOW_MILLISECONDS[window]).toISOString()
    Promise.all([
      getServiceMetrics(service.id, window),
      getHealthChecks(service.id, { from, limit: 500 }),
    ])
      .then(([nextMetrics, nextHistory]) => {
        if (cancelled) return
        setMetrics(nextMetrics)
        setHistory(nextHistory)
      })
      .catch((caught: unknown) => {
        if (!cancelled) setError(caught instanceof Error ? caught.message : 'Could not load service telemetry')
      })
    return () => {
      cancelled = true
    }
  }, [service.id, window, refreshVersion, localVersion])

  async function handleChanged() {
    await onChanged()
    setLocalVersion((version) => version + 1)
  }

  const serviceAlerts = alerts.filter(
    (alert) => alert.serviceId === service.id && alert.status !== 'RESOLVED',
  )

  return (
    <div className="service-detail">
      <button type="button" className="back-link" onClick={onBack}>
        <Icon name="arrow-left" className="h-4 w-4" /> Back to overview
      </button>

      <header className="service-detail__header">
        <div className="service-detail__identity">
          <span className="service-favicon service-favicon--large" aria-hidden="true">
            {service.name.slice(0, 2).toUpperCase()}
          </span>
          <div>
            <div className="service-detail__title-row">
              <h1>{service.name}</h1>
              <StatusBadge status={service.currentStatus} />
              {underMaintenance && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-purple-100 px-2.5 py-0.5 text-xs font-semibold text-purple-800">
                  <span className="h-1.5 w-1.5 rounded-full bg-purple-500" aria-hidden="true" />
                  Maintenance
                </span>
              )}
            </div>
            <a href={service.healthUrl} target="_blank" rel="noreferrer">
              {service.healthUrl} <Icon name="external" className="h-3.5 w-3.5" />
            </a>
          </div>
        </div>
        <div className="window-switcher" aria-label="Metrics window">
          {WINDOWS.map((value) => (
            <button
              type="button"
              key={value}
              className={window === value ? 'window-switcher__active' : ''}
              aria-pressed={window === value}
              onClick={() => setWindow(value)}
            >
              {value}
            </button>
          ))}
        </div>
      </header>

      {error && <div className="page-error" role="alert">Could not load telemetry: {error}</div>}

      <section className="metric-grid metric-grid--detail" aria-label={`${service.name} metrics for ${window}`}>
        <article className="metric-card metric-card--featured">
          <p>Availability</p>
          <strong>{metrics ? `${metrics.availabilityPercent.toFixed(2)}%` : '—'}</strong>
          <span>{metrics ? `${metrics.successfulChecks} of ${metrics.totalChecks} checks passed` : 'Loading window…'}</span>
        </article>
        <article className="metric-card">
          <p>Average response</p>
          <strong>{formatLatency(metrics?.averageResponseTimeMs ?? null)}</strong>
          <span>Successful requests</span>
        </article>
        <article className="metric-card">
          <p>p95 response</p>
          <strong>{formatLatency(metrics?.p95ResponseTimeMs ?? null)}</strong>
          <span>{metrics?.p95Sampled ? `Latest ${metrics.p95SampleSize} checks sampled` : 'Complete window sample'}</span>
        </article>
        <article className="metric-card">
          <p>Failed checks</p>
          <strong>{metrics?.failedChecks ?? '—'}</strong>
          <span>{serviceAlerts.length} active {serviceAlerts.length === 1 ? 'incident' : 'incidents'}</span>
        </article>
      </section>

      <div className="service-detail__primary-grid">
        <section className="panel chart-panel" aria-labelledby="response-chart-title">
          <div className="panel__header">
            <div>
              <p className="eyebrow">Latency</p>
              <h2 id="response-chart-title">Response time</h2>
            </div>
            <span className="panel__meta">{history?.length ?? 0} plotted checks</span>
          </div>
          <Suspense fallback={<div className="chart-empty" role="status"><p>Loading chart…</p></div>}>
            <ResponseTimeChart results={history ?? []} p95={metrics?.p95ResponseTimeMs ?? null} />
          </Suspense>
        </section>

        <aside className="panel configuration-panel" aria-labelledby="configuration-title">
          <div className="panel__header">
            <div>
              <p className="eyebrow">Controls</p>
              <h2 id="configuration-title">Monitor configuration</h2>
            </div>
          </div>
          <dl className="configuration-list">
            <div><dt>Last checked</dt><dd>{formatDateTime(service.lastCheckedAt)}</dd></div>
            <div><dt>Interval</dt><dd>{service.checkIntervalSeconds} seconds</dd></div>
            <div><dt>Monitor type</dt><dd>{service.monitorType}</dd></div>
          </dl>
          <EvaluationPolicyControl service={service} onChanged={handleChanged} />
          <ReliabilityPolicyControl service={service} onChanged={handleChanged} />
          <SloTargetControl service={service} onChanged={handleChanged} />
          <ServiceControls service={service} onChanged={handleChanged} />
          <SslCertificateStatus service={service} />
          <MaintenanceWindowControl
            service={service}
            onChanged={handleChanged}
            onActiveWindowChange={setUnderMaintenance}
          />
        </aside>
      </div>

      <section className="panel pulse-panel" aria-labelledby="pulse-title">
        <div className="panel__header">
          <div>
            <p className="eyebrow">Check sequence</p>
            <h2 id="pulse-title">Service pulse</h2>
          </div>
          <span className="panel__meta">Height represents latency · color represents lifecycle state</span>
        </div>
        <PulseRail results={history ?? []} />
      </section>

      <div className="service-detail__timeline-grid">
        <section className="panel timeline-panel" aria-labelledby="health-timeline-heading">
          <div className="panel__header">
            <div><p className="eyebrow">Evidence</p><h2 id="health-timeline-heading">Health checks</h2></div>
          </div>
          <HealthCheckTimeline serviceId={service.id} reloadKey={localVersion + refreshVersion} />
        </section>
        <section className="panel timeline-panel" aria-labelledby="event-timeline-heading">
          <div className="panel__header">
            <div><p className="eyebrow">Lifecycle</p><h2 id="event-timeline-heading">Status transitions</h2></div>
          </div>
          <StatusTimeline serviceId={service.id} reloadKey={localVersion + refreshVersion} />
        </section>
      </div>
    </div>
  )
}
