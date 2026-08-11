import type {
  AlertResponse,
  HealthCheckResultResponse,
  MonitoredService,
  ServiceMetricsResponse,
} from '../types'
import { formatDateTime, formatLatency, formatRelativeTime, statusTone } from '../lib/format'
import { Icon } from './Icon'
import { PulseRail } from './PulseRail'
import { StatusBadge } from './StatusBadge'

interface DashboardOverviewProps {
  services: MonitoredService[]
  alerts: AlertResponse[]
  metrics: Record<string, ServiceMetricsResponse>
  histories: Record<string, HealthCheckResultResponse[]>
  insightsLoading: boolean
  onSelectService: (serviceId: string) => void
  onAddMonitor: () => void
  onShowIncidents: () => void
}

function fleetAvailability(metrics: ServiceMetricsResponse[]): number | null {
  const totals = metrics.reduce(
    (aggregate, metric) => ({
      checks: aggregate.checks + metric.totalChecks,
      successes: aggregate.successes + metric.successfulChecks,
    }),
    { checks: 0, successes: 0 },
  )
  return totals.checks === 0 ? null : (totals.successes / totals.checks) * 100
}

export function DashboardOverview({
  services,
  alerts,
  metrics,
  histories,
  insightsLoading,
  onSelectService,
  onAddMonitor,
  onShowIncidents,
}: DashboardOverviewProps) {
  const activeAlerts = alerts.filter((alert) => alert.status !== 'RESOLVED')
  const down = services.filter((service) => service.currentStatus === 'DOWN').length
  const degraded = services.filter((service) => service.currentStatus === 'DEGRADED').length
  const enabled = services.filter((service) => service.enabled).length
  const metricValues = Object.values(metrics)
  const availability = fleetAvailability(metricValues)
  const successfulLatencySamples = metricValues.reduce(
    (sum, metric) => sum + (metric.averageResponseTimeMs === null ? 0 : metric.successfulChecks),
    0,
  )
  const averageLatency = successfulLatencySamples > 0
    ? metricValues.reduce(
        (sum, metric) => sum + (metric.averageResponseTimeMs ?? 0) * metric.successfulChecks,
        0,
      ) / successfulLatencySamples
    : null

  const hero = down > 0
    ? {
        eyebrow: 'Incident response active',
        title: `${down} ${down === 1 ? 'endpoint is' : 'endpoints are'} down`,
        copy: 'CloudPulse is capturing the failure path and keeping the incident trail current.',
        tone: 'incident',
      }
    : degraded > 0
      ? {
          eyebrow: 'Performance watch',
          title: `${degraded} ${degraded === 1 ? 'service needs' : 'services need'} attention`,
          copy: 'Availability is intact, but response times have crossed an evaluation threshold.',
          tone: 'warning',
        }
      : {
          eyebrow: 'Fleet in rhythm',
          title: 'Every monitored system is operational',
          copy: 'Checks are arriving on schedule. CloudPulse will surface the first meaningful change.',
          tone: 'nominal',
        }

  if (services.length === 0) {
    return (
      <section className="empty-onboarding" aria-labelledby="empty-title">
        <div className="empty-onboarding__signal" aria-hidden="true">
          <span />
          <span />
          <span />
          <span />
          <span />
        </div>
        <p className="eyebrow">Your first signal</p>
        <h2 id="empty-title">Put a website or API on watch.</h2>
        <p>
          Paste a public URL. CloudPulse tests it safely, detects its response format, and starts building an operational history.
        </p>
        <button type="button" className="button button--primary" onClick={onAddMonitor}>
          <Icon name="plus" className="h-4 w-4" />
          Add your first monitor
        </button>
      </section>
    )
  }

  return (
    <div className="space-y-7">
      <section className={`fleet-hero fleet-hero--${hero.tone}`} aria-labelledby="fleet-title">
        <div className="fleet-hero__copy">
          <p className="eyebrow">{hero.eyebrow}</p>
          <h2 id="fleet-title">{hero.title}</h2>
          <p>{hero.copy}</p>
        </div>
        <div className="fleet-hero__scope" aria-label="Monitoring coverage">
          <div>
            <span className="data-label">Watching</span>
            <strong>{enabled}/{services.length}</strong>
            <span>endpoints</span>
          </div>
          <div className="fleet-hero__orbit" aria-hidden="true">
            <span />
          </div>
        </div>
      </section>

      <section className="metric-grid" aria-label="Fleet metrics for the last 24 hours">
        <article className="metric-card">
          <span className="metric-card__marker metric-card__marker--blue" />
          <p>Fleet availability</p>
          <strong>{availability === null ? '—' : `${availability.toFixed(2)}%`}</strong>
          <span>{insightsLoading ? 'Calculating…' : 'Across all completed checks'}</span>
        </article>
        <article className="metric-card">
          <span className="metric-card__marker metric-card__marker--teal" />
          <p>Average response</p>
          <strong>{formatLatency(averageLatency)}</strong>
          <span>Reachable checks only</span>
        </article>
        <article className="metric-card">
          <span className="metric-card__marker metric-card__marker--coral" />
          <p>Active incidents</p>
          <strong>{activeAlerts.length}</strong>
          <button type="button" onClick={onShowIncidents}>
            Review incident queue <Icon name="arrow-right" className="h-3.5 w-3.5" />
          </button>
        </article>
        <article className="metric-card">
          <span className="metric-card__marker metric-card__marker--amber" />
          <p>Checks observed</p>
          <strong>{metricValues.reduce((sum, metric) => sum + metric.totalChecks, 0)}</strong>
          <span>Rolling 24-hour window</span>
        </article>
      </section>

      <section aria-labelledby="service-pulse-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Live pulse</p>
            <h2 id="service-pulse-title">Service signals</h2>
          </div>
          <span className="section-heading__note">Newest check on the right</span>
        </div>

        <div className="service-signal-grid">
          {services.map((service) => {
            const serviceMetrics = metrics[service.id]
            const history = histories[service.id] ?? []
            return (
              <button
                type="button"
                key={service.id}
                className={`service-signal-card service-signal-card--${statusTone(service.currentStatus)}`}
                onClick={() => onSelectService(service.id)}
                aria-label={`Open ${service.name} details`}
              >
                <div className="service-signal-card__top">
                  <div className="service-signal-card__identity">
                    <span className="service-favicon" aria-hidden="true">
                      {service.name.slice(0, 2).toUpperCase()}
                    </span>
                    <div>
                      <h3>{service.name}</h3>
                      <p>{service.enabled ? `Checks every ${service.checkIntervalSeconds}s` : 'Monitoring paused'}</p>
                    </div>
                  </div>
                  <StatusBadge status={service.currentStatus} />
                </div>
                <PulseRail results={history} compact />
                <dl className="service-signal-card__metrics">
                  <div>
                    <dt>Availability</dt>
                    <dd>{serviceMetrics ? `${serviceMetrics.availabilityPercent.toFixed(2)}%` : '—'}</dd>
                  </div>
                  <div>
                    <dt>p95 latency</dt>
                    <dd>{formatLatency(serviceMetrics?.p95ResponseTimeMs ?? null)}</dd>
                  </div>
                  <div>
                    <dt>Last check</dt>
                    <dd>{formatRelativeTime(service.lastCheckedAt)}</dd>
                  </div>
                </dl>
                <span className="service-signal-card__open">
                  Inspect signal <Icon name="arrow-right" className="h-4 w-4" />
                </span>
              </button>
            )
          })}
        </div>
      </section>

      <section className="incident-preview" aria-labelledby="incident-preview-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Operator queue</p>
            <h2 id="incident-preview-title">Recent incidents</h2>
          </div>
          <button type="button" className="text-action" onClick={onShowIncidents}>
            View all <Icon name="arrow-right" className="h-4 w-4" />
          </button>
        </div>
        {alerts.length === 0 ? (
          <div className="quiet-state">
            <Icon name="check" className="h-5 w-5" />
            <span>No incidents have been recorded.</span>
          </div>
        ) : (
          <div className="incident-preview__list">
            {alerts.slice(0, 4).map((alert) => (
              <article key={alert.id}>
                <span className={`incident-dot incident-dot--${alert.severity.toLowerCase()}`} />
                <div>
                  <h3>{alert.serviceName ?? 'Unknown service'}</h3>
                  <p>{alert.message}</p>
                </div>
                <div className="incident-preview__meta">
                  <span>{alert.status.toLowerCase()}</span>
                  <time dateTime={alert.createdAt}>{formatDateTime(alert.createdAt)}</time>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
