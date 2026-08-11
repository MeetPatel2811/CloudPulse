interface StatusStyle {
  label: string
  badgeClassName: string
  accentClassName: string
  dotClassName: string
  leftBorderClassName: string
}

// Keyed by the exact ServiceStatus enum values from
// backend/src/main/java/com/cloudpulse/model/ServiceStatus.java.
export const STATUS_STYLES: Record<string, StatusStyle> = {
  HEALTHY: {
    label: 'Healthy',
    badgeClassName: 'bg-green-100 text-green-800',
    accentClassName: 'border-green-200 bg-green-50 text-green-700',
    dotClassName: 'bg-green-500',
    leftBorderClassName: 'border-l-green-500',
  },
  DEGRADED: {
    label: 'Degraded',
    badgeClassName: 'bg-amber-100 text-amber-800',
    accentClassName: 'border-amber-200 bg-amber-50 text-amber-700',
    dotClassName: 'bg-amber-500',
    leftBorderClassName: 'border-l-amber-500',
  },
  DOWN: {
    label: 'Down',
    badgeClassName: 'bg-red-100 text-red-800',
    accentClassName: 'border-red-200 bg-red-50 text-red-700',
    dotClassName: 'bg-red-500',
    leftBorderClassName: 'border-l-red-500',
  },
  RECOVERED: {
    label: 'Recovered',
    badgeClassName: 'bg-blue-100 text-blue-800',
    accentClassName: 'border-blue-200 bg-blue-50 text-blue-700',
    dotClassName: 'bg-blue-500',
    leftBorderClassName: 'border-l-blue-500',
  },
  UNKNOWN: {
    label: 'Unknown',
    badgeClassName: 'bg-gray-100 text-gray-800',
    accentClassName: 'border-gray-200 bg-gray-50 text-gray-700',
    dotClassName: 'bg-gray-400',
    leftBorderClassName: 'border-l-gray-400',
  },
}

export function statusStyle(status: string): StatusStyle {
  return STATUS_STYLES[status.toUpperCase()] ?? STATUS_STYLES.UNKNOWN
}

export function StatusBadge({ status }: { status: string }) {
  const { label, badgeClassName, dotClassName } = statusStyle(status)
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${badgeClassName}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotClassName}`} aria-hidden="true" />
      {label}
    </span>
  )
}
