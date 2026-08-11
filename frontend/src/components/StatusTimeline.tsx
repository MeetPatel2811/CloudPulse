import { useEffect, useState } from 'react'
import { getStatusEvents } from '../api/statusEvents'
import type { StatusEventResponse } from '../types'
import { statusStyle } from './StatusBadge'

function formatTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

// The recorded lifecycle transitions for one service, newest first.
// reloadKey lets the parent force a re-pull after a manual check adds an event.
export function StatusTimeline({ serviceId, reloadKey = 0 }: { serviceId: string; reloadKey?: number }) {
  const [events, setEvents] = useState<StatusEventResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setEvents(null)
    setError(null)

    getStatusEvents(serviceId)
      .then((data) => {
        if (!cancelled) setEvents(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load status events')
      })

    return () => {
      cancelled = true
    }
  }, [serviceId, reloadKey])

  if (error) {
    return <div role="alert" className="py-3 text-sm text-red-700">Could not load timeline: {error}</div>
  }

  if (events === null) {
    return <div role="status" aria-live="polite" className="py-3 text-sm text-gray-500">Loading timeline…</div>
  }

  if (events.length === 0) {
    return <div role="status" className="py-3 text-sm text-gray-500">No status changes recorded yet.</div>
  }

  return (
    <ol aria-label="Status change history" className="space-y-3">
      {events.map((event) => {
        const to = statusStyle(event.newStatus)
        return (
          <li
            key={event.id}
            className={`rounded-md border border-l-4 border-gray-200 bg-white px-4 py-2.5 ${to.leftBorderClassName}`}
          >
            <div className="flex items-center gap-2 text-sm font-medium text-gray-900">
              <span className="text-gray-500">{event.previousStatus}</span>
              <span aria-hidden="true">→</span>
              <span className="sr-only">changed to</span>
              <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${to.badgeClassName}`}>
                {to.label}
              </span>
            </div>
            <p className="mt-0.5 text-xs text-gray-600">
              <time dateTime={event.occurredAt}>{formatTime(event.occurredAt)}</time>
            </p>
            {event.reason && <p className="mt-1 text-xs text-gray-500">{event.reason}</p>}
          </li>
        )
      })}
    </ol>
  )
}
