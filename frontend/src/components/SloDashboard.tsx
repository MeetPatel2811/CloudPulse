import type { MetricsWindow, MonitoredService, ServiceMetricsResponse } from '../types'
import { buildSloCsv, downloadCsv } from '../lib/csvReport'
import { Icon } from './Icon'

const WINDOWS: MetricsWindow[] = ['1h', '6h', '24h', '7d']

function budgetTone(metric: ServiceMetricsResponse): string {
  if (metric.totalChecks === 0) return 'empty'
  if (!metric.sloMet) return 'exhausted'
  if (metric.errorBudgetRemainingPercent < 25) return 'warning'
  return 'healthy'
}

export function SloDashboard({
  services,
  metrics,
  window,
  loading,
  onWindowChange,
  onSelectService,
}: {
  services: MonitoredService[]
  metrics: Record<string, ServiceMetricsResponse>
  window: MetricsWindow
  loading: boolean
  onWindowChange: (window: MetricsWindow) => void
  onSelectService: (serviceId: string) => void
}) {
  const rows = services.map((service) => ({ service, metric: metrics[service.id] })).filter(
    (row): row is { service: MonitoredService; metric: ServiceMetricsResponse } => Boolean(row.metric),
  )
  const measured = rows.filter(({ metric }) => metric.totalChecks > 0)
  const objectivesMet = measured.filter(({ metric }) => metric.sloMet).length
  const exhausted = measured.filter(({ metric }) => metric.errorBudgetRemainingPercent === 0).length
  const totalChecks = measured.reduce((sum, { metric }) => sum + metric.totalChecks, 0)
  const successfulChecks = measured.reduce((sum, { metric }) => sum + metric.successfulChecks, 0)
  const fleetAvailability = totalChecks === 0 ? null : successfulChecks / totalChecks * 100

  function exportReport() {
    const date = new Date().toISOString().slice(0, 10)
    downloadCsv(`cloudpulse-slo-${window}-${date}.csv`, buildSloCsv(services, metrics, window))
  }

  return (
    <div className="slo-dashboard">
      <header className="view-header slo-dashboard__header">
        <div>
          <p className="eyebrow">Reliability reporting</p>
          <h1>SLO &amp; error budgets</h1>
          <p>Track availability objectives and the remaining failure tolerance for every service.</p>
        </div>
        <div className="slo-dashboard__actions">
          <div className="window-switcher" aria-label="SLO reporting window">
            {WINDOWS.map((value) => (
              <button
                type="button"
                key={value}
                className={window === value ? 'window-switcher__active' : ''}
                aria-pressed={window === value}
                onClick={() => onWindowChange(value)}
              >
                {value}
              </button>
            ))}
          </div>
          <button type="button" className="button button--secondary" onClick={exportReport} disabled={services.length === 0}>
            <Icon name="download" className="h-4 w-4" /> Export CSV
          </button>
        </div>
      </header>

      <section className="metric-grid" aria-label={`Fleet SLO summary for ${window}`} aria-busy={loading}>
        <article className="metric-card"><p>Objectives met</p><strong>{objectivesMet}/{measured.length}</strong><span>Measured services</span></article>
        <article className="metric-card"><p>Fleet availability</p><strong>{fleetAvailability === null ? '—' : `${fleetAvailability.toFixed(2)}%`}</strong><span>Weighted by checks</span></article>
        <article className="metric-card"><p>Budgets exhausted</p><strong>{exhausted}</strong><span>Needs operator attention</span></article>
        <article className="metric-card"><p>Checks included</p><strong>{totalChecks}</strong><span>Current {window} window</span></article>
      </section>

      <section className="panel slo-table-panel" aria-labelledby="slo-table-title">
        <div className="panel__header">
          <div><p className="eyebrow">Service objectives</p><h2 id="slo-table-title">Error-budget position</h2></div>
          <span className="panel__meta">100% means the full budget remains</span>
        </div>
        {services.length === 0 ? (
          <div className="quiet-state"><Icon name="pulse" className="h-5 w-5" /><span>Add a monitor to begin SLO reporting.</span></div>
        ) : (
          <div className="slo-table-wrap">
            <table className="slo-table">
              <caption className="sr-only">Availability SLO and error budget by service</caption>
              <thead><tr><th scope="col">Service</th><th scope="col">Objective</th><th scope="col">Actual</th><th scope="col">Error budget remaining</th><th scope="col">Result</th></tr></thead>
              <tbody>
                {services.map((service) => {
                  const metric = metrics[service.id]
                  const tone = metric ? budgetTone(metric) : 'empty'
                  return (
                    <tr key={service.id}>
                      <td>
                        <button type="button" className="slo-service-link" onClick={() => onSelectService(service.id)}>
                          <strong>{service.name}</strong><span>{service.serviceGroup ?? 'Ungrouped'}</span>
                        </button>
                      </td>
                      <td>{service.availabilitySloPercent}%</td>
                      <td>{metric && metric.totalChecks > 0 ? `${metric.availabilityPercent.toFixed(2)}%` : 'No data'}</td>
                      <td>
                        <div className={`budget-meter budget-meter--${tone}`}>
                          <div><span style={{ width: `${metric?.errorBudgetRemainingPercent ?? 0}%` }} /></div>
                          <strong>{metric ? `${metric.errorBudgetRemainingPercent.toFixed(1)}%` : '—'}</strong>
                        </div>
                      </td>
                      <td><span className={`slo-result slo-result--${tone}`}>{!metric || metric.totalChecks === 0 ? 'Awaiting data' : metric.sloMet ? 'On target' : 'Breached'}</span></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
