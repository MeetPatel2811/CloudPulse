import { useMemo, useState } from 'react'
import { deleteService, updateService } from '../api/services'
import type {
  HealthCheckResultResponse,
  MonitoredService,
  RegisterServiceRequest,
  ServiceMetricsResponse,
} from '../types'
import { formatLatency, formatRelativeTime } from '../lib/format'
import { ConfirmDialog } from './ConfirmDialog'
import { Icon } from './Icon'
import { PulseRail } from './PulseRail'
import { ServiceForm } from './ServiceForm'
import { StatusBadge } from './StatusBadge'

export function ServiceDirectory({
  services,
  metrics,
  histories,
  onAddMonitor,
  onSelectService,
  onChanged,
}: {
  services: MonitoredService[]
  metrics: Record<string, ServiceMetricsResponse>
  histories: Record<string, HealthCheckResultResponse[]>
  onAddMonitor: () => void
  onSelectService: (serviceId: string) => void
  onChanged: () => void | Promise<void>
}) {
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [groupFilter, setGroupFilter] = useState('ALL')
  const [tagFilter, setTagFilter] = useState('ALL')
  const [editing, setEditing] = useState<MonitoredService | null>(null)
  const [deleting, setDeleting] = useState<MonitoredService | null>(null)
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const groups = useMemo(
    () => Array.from(new Set(services.map((service) => service.serviceGroup).filter((group): group is string => Boolean(group)))).sort(),
    [services],
  )
  const tags = useMemo(
    () => Array.from(new Set(services.flatMap((service) => service.tags))).sort(),
    [services],
  )

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return services.filter((service) => {
      const matchesQuery = !normalizedQuery
        || service.name.toLowerCase().includes(normalizedQuery)
        || service.healthUrl.toLowerCase().includes(normalizedQuery)
        || service.serviceGroup?.toLowerCase().includes(normalizedQuery)
        || service.tags.some((tag) => tag.includes(normalizedQuery))
      const matchesStatus = statusFilter === 'ALL' || service.currentStatus === statusFilter
      const matchesGroup = groupFilter === 'ALL' || service.serviceGroup === groupFilter
      const matchesTag = tagFilter === 'ALL' || service.tags.includes(tagFilter)
      return matchesQuery && matchesStatus && matchesGroup && matchesTag
    })
  }, [groupFilter, query, services, statusFilter, tagFilter])

  const hasActiveFilters = Boolean(query.trim())
    || statusFilter !== 'ALL'
    || groupFilter !== 'ALL'
    || tagFilter !== 'ALL'

  function clearFilters() {
    setQuery('')
    setStatusFilter('ALL')
    setGroupFilter('ALL')
    setTagFilter('ALL')
  }

  async function handleEdit(request: RegisterServiceRequest) {
    if (!editing) return
    await updateService(editing.id, request)
    setEditing(null)
    await onChanged()
  }

  async function handleDelete() {
    if (!deleting) return
    setDeleteBusy(true)
    setError(null)
    try {
      await deleteService(deleting.id)
      setDeleting(null)
      await onChanged()
    } catch (caught: unknown) {
      setDeleting(null)
      setError(caught instanceof Error ? caught.message : 'CloudPulse could not delete this monitor')
    } finally {
      setDeleteBusy(false)
    }
  }

  return (
    <div className="directory-view">
      {editing && (
        <ServiceForm
          title="Edit monitor"
          submitLabel="Save changes"
          initial={{
            name: editing.name,
            healthUrl: editing.healthUrl,
            monitorType: editing.monitorType,
            checkIntervalSeconds: editing.checkIntervalSeconds as RegisterServiceRequest['checkIntervalSeconds'],
            expectedKeyword: editing.expectedKeyword ?? undefined,
            serviceGroup: editing.serviceGroup,
            tags: editing.tags,
          }}
          onSubmit={handleEdit}
          onClose={() => setEditing(null)}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete monitor"
          message={`Delete “${deleting.name}”? Monitors with recorded history must be disabled instead.`}
          confirmLabel="Delete monitor"
          busy={deleteBusy}
          onConfirm={handleDelete}
          onCancel={() => setDeleting(null)}
        />
      )}

      <header className="view-header">
        <div>
          <p className="eyebrow">Inventory</p>
          <h1>Monitored services</h1>
          <p>Every endpoint, its current signal, and the context needed to investigate it.</p>
        </div>
        <button type="button" className="button button--primary" onClick={onAddMonitor}>
          <Icon name="plus" className="h-4 w-4" /> Add monitor
        </button>
      </header>

      <div className="directory-toolbar">
        <label className="search-control">
          <span className="sr-only">Search monitors</span>
          <Icon name="search" className="h-4 w-4" />
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search name, URL, group, or tag"
          />
        </label>
        <div className="directory-filters" aria-label="Service filters">
          <label className="filter-control">
            <span>Status</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">All statuses</option>
              <option value="HEALTHY">Healthy</option>
              <option value="DEGRADED">Degraded</option>
              <option value="DOWN">Down</option>
              <option value="RECOVERED">Recovered</option>
              <option value="UNKNOWN">Unknown</option>
            </select>
          </label>
          <label className="filter-control">
            <span>Group</span>
            <select value={groupFilter} onChange={(event) => setGroupFilter(event.target.value)}>
              <option value="ALL">All groups</option>
              {groups.map((group) => <option key={group} value={group}>{group}</option>)}
            </select>
          </label>
          <label className="filter-control">
            <span>Tag</span>
            <select value={tagFilter} onChange={(event) => setTagFilter(event.target.value)}>
              <option value="ALL">All tags</option>
              {tags.map((tag) => <option key={tag} value={tag}>{tag}</option>)}
            </select>
          </label>
          {hasActiveFilters && <button type="button" className="clear-filters" onClick={clearFilters}>Clear</button>}
        </div>
      </div>

      {error && <div role="alert" className="page-error">{error}</div>}

      {filtered.length === 0 ? (
        <div className="quiet-state quiet-state--large" role="status">
          <Icon name="search" className="h-5 w-5" />
          <span>{services.length === 0 ? 'No monitors have been added yet.' : 'No monitors match these filters.'}</span>
        </div>
      ) : (
        <div className="directory-table-wrap">
          <table className="directory-table">
            <caption className="sr-only">CloudPulse monitored services</caption>
            <thead>
              <tr>
                <th scope="col">Service</th>
                <th scope="col">Signal</th>
                <th scope="col">Availability</th>
                <th scope="col">p95</th>
                <th scope="col">Last check</th>
                <th scope="col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((service) => {
                const serviceMetrics = metrics[service.id]
                return (
                  <tr key={service.id}>
                    <td>
                      <button type="button" className="directory-service" onClick={() => onSelectService(service.id)}>
                        <span className="service-favicon" aria-hidden="true">{service.name.slice(0, 2).toUpperCase()}</span>
                        <span>
                          <strong>{service.name}</strong>
                          <small>{service.healthUrl}</small>
                          {(service.serviceGroup || service.tags.length > 0) && (
                            <span className="service-metadata">
                              {service.serviceGroup && <span className="service-group-chip">{service.serviceGroup}</span>}
                              {service.tags.map((tag) => <span key={tag} className="service-tag-chip">#{tag}</span>)}
                            </span>
                          )}
                        </span>
                      </button>
                    </td>
                    <td>
                      <div className="directory-signal">
                        <StatusBadge status={service.currentStatus} />
                        <PulseRail results={histories[service.id] ?? []} compact />
                      </div>
                    </td>
                    <td className="data-cell">{serviceMetrics ? `${serviceMetrics.availabilityPercent.toFixed(2)}%` : '—'}</td>
                    <td className="data-cell">{formatLatency(serviceMetrics?.p95ResponseTimeMs ?? null)}</td>
                    <td>{formatRelativeTime(service.lastCheckedAt)}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" onClick={() => setEditing(service)}>Edit</button>
                        <button type="button" className="row-actions__danger" onClick={() => setDeleting(service)}>Delete</button>
                        <button type="button" className="icon-button" aria-label={`Open ${service.name}`} onClick={() => onSelectService(service.id)}>
                          <Icon name="arrow-right" className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
