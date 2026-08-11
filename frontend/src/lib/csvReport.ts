import type { MetricsWindow, MonitoredService, ServiceMetricsResponse } from '../types'

function csvCell(value: string | number | boolean | null | undefined): string {
  if (value === null || value === undefined) return ''
  let text = String(value)
  if (/^[=+\-@]/.test(text)) text = `'${text}`
  return `"${text.replaceAll('"', '""')}"`
}

export function buildSloCsv(
  services: MonitoredService[],
  metrics: Record<string, ServiceMetricsResponse>,
  window: MetricsWindow,
): string {
  const headers = [
    'Service',
    'Group',
    'Tags',
    'Monitoring enabled',
    'Current status',
    'Window',
    'SLO target (%)',
    'Total checks',
    'Successful checks',
    'Failed checks',
    'Availability (%)',
    'Error budget remaining (%)',
    'SLO result',
  ]
  const rows = services.map((service) => {
    const metric = metrics[service.id]
    return [
      service.name,
      service.serviceGroup,
      service.tags.join('; '),
      service.enabled,
      service.currentStatus,
      window,
      service.availabilitySloPercent,
      metric?.totalChecks,
      metric?.successfulChecks,
      metric?.failedChecks,
      metric && metric.totalChecks > 0 ? metric.availabilityPercent : null,
      metric?.errorBudgetRemainingPercent,
      !metric || metric.totalChecks === 0 ? 'NO_DATA' : metric.sloMet ? 'MET' : 'BREACHED',
    ]
  })
  return [headers, ...rows].map((row) => row.map(csvCell).join(',')).join('\r\n')
}

export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}
