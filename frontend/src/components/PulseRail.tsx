import type { HealthCheckResultResponse } from '../types'
import { statusTone } from '../lib/format'

export function PulseRail({
  results,
  compact = false,
}: {
  results: HealthCheckResultResponse[]
  compact?: boolean
}) {
  const chronological = [...results].reverse().slice(-30)
  const maxLatency = Math.max(
    1,
    ...chronological.filter((result) => result.reachable).map((result) => result.responseTimeMs),
  )
  const successful = chronological.filter((result) => result.reachable).length

  if (chronological.length === 0) {
    return (
      <div className={`pulse-rail pulse-rail--empty ${compact ? 'pulse-rail--compact' : ''}`}>
        <span>No pulse data yet</span>
      </div>
    )
  }

  return (
    <div
      className={`pulse-rail ${compact ? 'pulse-rail--compact' : ''}`}
      role="img"
      aria-label={`${successful} of the last ${chronological.length} checks were reachable`}
    >
      {chronological.map((result) => {
        const latencyRatio = result.reachable ? result.responseTimeMs / maxLatency : 1
        const height = compact ? 10 + latencyRatio * 16 : 14 + latencyRatio * 30
        return (
          <span
            key={result.id}
            className={`pulse-bar pulse-bar--${statusTone(result.evaluatedStatus)}`}
            style={{ height: `${height}px` }}
            title={`${result.evaluatedStatus} · ${result.reachable ? `${result.responseTimeMs} ms` : 'unreachable'}`}
          />
        )
      })}
    </div>
  )
}
