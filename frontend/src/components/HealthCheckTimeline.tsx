import { useEffect, useState } from 'react'
import { getHealthChecks } from '../api/healthChecks'
import type { HealthCheckResultResponse } from '../types'
import { statusStyle } from './StatusBadge'

const MAX_VISIBLE_RESULTS = 20

function formatTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

export function HealthCheckTimeline({
  serviceId,
  reloadKey = 0,
}: {
  serviceId: string
  reloadKey?: number
}) {
  const [results, setResults] = useState<HealthCheckResultResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setResults(null)
    setError(null)

    getHealthChecks(serviceId)
      .then((data) => {
        if (!cancelled) setResults(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load health checks')
      })

    return () => {
      cancelled = true
    }
  }, [serviceId, reloadKey])

  if (error) {
    return <div role="alert" className="py-3 text-sm text-red-700">Could not load health history: {error}</div>
  }

  if (results === null) {
    return <div role="status" aria-live="polite" className="py-3 text-sm text-gray-500">Loading health history…</div>
  }

  if (results.length === 0) {
    return <div role="status" className="py-3 text-sm text-gray-500">No health checks recorded yet.</div>
  }

  const visibleResults = results.slice(0, MAX_VISIBLE_RESULTS)

  return (
    <div className="space-y-3">
      {results.length > MAX_VISIBLE_RESULTS && (
        <p className="text-xs text-gray-500">
          Showing the newest {MAX_VISIBLE_RESULTS} of {results.length} checks.
        </p>
      )}
      <ol aria-label="Health-check history" className="space-y-3">
        {visibleResults.map((result) => {
          const status = statusStyle(result.evaluatedStatus)
          const httpStatus = result.httpStatus > 0 ? String(result.httpStatus) : 'Not available'

          return (
            <li
              key={result.id}
              className={`rounded-md border border-l-4 border-gray-200 bg-white px-4 py-3 ${status.leftBorderClassName}`}
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${status.badgeClassName}`}>
                  {status.label}
                </span>
                <time className="text-xs text-gray-600" dateTime={result.checkedAt}>
                  {formatTime(result.checkedAt)}
                </time>
              </div>

              <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs sm:grid-cols-3">
                <div>
                  <dt className="text-gray-500">Reachability</dt>
                  <dd className={`font-medium ${result.reachable ? 'text-green-700' : 'text-red-700'}`}>
                    {result.reachable ? 'Reachable' : 'Unreachable'}
                  </dd>
                </div>
                <div>
                  <dt className="text-gray-500">HTTP status</dt>
                  <dd className="font-medium text-gray-800">{httpStatus}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Response time</dt>
                  <dd className="font-medium text-gray-800">
                    {result.reachable ? `${result.responseTimeMs} ms` : 'No response'}
                  </dd>
                </div>
              </dl>

              {result.rawMessage && (
                <p className="mt-2 break-words text-xs text-gray-500">{result.rawMessage}</p>
              )}
            </li>
          )
        })}
      </ol>
    </div>
  )
}
