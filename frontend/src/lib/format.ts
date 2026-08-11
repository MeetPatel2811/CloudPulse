import type { ServiceStatus } from '../types'

export function formatRelativeTime(value: string | Date | null): string {
  if (!value) return 'Not checked yet'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)

  const seconds = Math.round((date.getTime() - Date.now()) / 1000)
  const absolute = Math.abs(seconds)
  const formatter = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' })
  if (absolute < 60) return formatter.format(seconds, 'second')
  if (absolute < 3_600) return formatter.format(Math.round(seconds / 60), 'minute')
  if (absolute < 86_400) return formatter.format(Math.round(seconds / 3_600), 'hour')
  return formatter.format(Math.round(seconds / 86_400), 'day')
}

export function formatRefreshAge(elapsedSeconds: number): string {
  const seconds = Math.max(0, Math.floor(elapsedSeconds))
  if (seconds < 60) return `${seconds}s ago`

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`

  return `${Math.floor(minutes / 60)}h ago`
}

export function formatDateTime(value: string | null): string {
  if (!value) return 'Not available'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      }).format(date)
}

export function formatLatency(value: number | null): string {
  if (value === null) return '—'
  if (value >= 1_000) return `${(value / 1_000).toFixed(value >= 10_000 ? 0 : 1)} s`
  return `${Math.round(value)} ms`
}

export function statusTone(status: ServiceStatus): string {
  switch (status) {
    case 'HEALTHY':
      return 'nominal'
    case 'RECOVERED':
      return 'recovered'
    case 'DEGRADED':
      return 'warning'
    case 'DOWN':
      return 'incident'
    default:
      return 'unknown'
  }
}
