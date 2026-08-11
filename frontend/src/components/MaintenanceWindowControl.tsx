import { useEffect, useState } from 'react'
import {
  cancelMaintenanceWindow,
  getMaintenanceWindows,
  scheduleMaintenanceWindow,
} from '../api/maintenanceWindows'
import type { MaintenanceWindowResponse, MonitoredService } from '../types'
import { formatDateTime } from '../lib/format'

const STATUS_LABEL: Record<MaintenanceWindowResponse['status'], string> = {
  SCHEDULED: 'Scheduled',
  ACTIVE: 'Maintenance active',
  ENDED: 'Ended',
}

export function MaintenanceWindowControl({
  service,
  onChanged,
  onActiveWindowChange,
}: {
  service: MonitoredService
  onChanged?: () => void | Promise<void>
  onActiveWindowChange?: (active: boolean) => void
}) {
  const [windows, setWindows] = useState<MaintenanceWindowResponse[] | null>(null)
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getMaintenanceWindows(service.id)
      .then((data) => {
        if (cancelled) return
        setWindows(data)
        onActiveWindowChange?.(data.some((w) => w.status === 'ACTIVE'))
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Could not load maintenance windows')
      })
    return () => {
      cancelled = true
    }
  }, [service.id])

  async function refresh() {
    const data = await getMaintenanceWindows(service.id)
    setWindows(data)
    onActiveWindowChange?.(data.some((w) => w.status === 'ACTIVE'))
  }

  async function handleSchedule(event: React.FormEvent) {
    event.preventDefault()
    if (!startsAt || !endsAt) return
    setError(null)
    setBusy(true)
    try {
      await scheduleMaintenanceWindow(service.id, {
        startsAt: new Date(startsAt).toISOString(),
        endsAt: new Date(endsAt).toISOString(),
        reason: reason.trim() || undefined,
      })
      setStartsAt('')
      setEndsAt('')
      setReason('')
      await refresh()
      await onChanged?.()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Could not schedule this maintenance window')
    } finally {
      setBusy(false)
    }
  }

  async function handleCancel(windowId: string) {
    setError(null)
    setBusy(true)
    try {
      await cancelMaintenanceWindow(service.id, windowId)
      await refresh()
      await onChanged?.()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Could not cancel this maintenance window')
    } finally {
      setBusy(false)
    }
  }

  const upcomingOrActive = (windows ?? []).filter((w) => w.status !== 'ENDED')

  return (
    <div aria-busy={busy} className="rounded-md border border-gray-200 bg-gray-50 p-3">
      <p className="text-sm font-medium text-gray-800">Maintenance windows</p>
      <p className="text-xs text-gray-500">
        Alerts are suppressed for this service while a window is active.
      </p>

      <form onSubmit={handleSchedule} className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
        <label className="text-xs text-gray-600">
          Starts
          <input
            type="datetime-local"
            required
            value={startsAt}
            onChange={(e) => setStartsAt(e.target.value)}
            className="mt-1 w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>
        <label className="text-xs text-gray-600">
          Ends
          <input
            type="datetime-local"
            required
            value={endsAt}
            onChange={(e) => setEndsAt(e.target.value)}
            className="mt-1 w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>
        <label className="text-xs text-gray-600 sm:col-span-2">
          Reason (optional)
          <input
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Database migration"
            className="mt-1 w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={busy}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {busy ? 'Saving…' : 'Schedule window'}
          </button>
        </div>
      </form>

      {error && <p role="alert" className="mt-2 text-xs text-red-700">{error}</p>}

      {upcomingOrActive.length > 0 && (
        <ul className="mt-3 space-y-2">
          {upcomingOrActive.map((window) => (
            <li
              key={window.id}
              className="flex items-center justify-between rounded-md border border-gray-200 bg-white px-2 py-1.5 text-xs"
            >
              <div>
                <span
                  className={
                    window.status === 'ACTIVE'
                      ? 'font-medium text-amber-700'
                      : 'font-medium text-gray-700'
                  }
                >
                  {STATUS_LABEL[window.status]}
                </span>
                <span className="ml-2 text-gray-600">
                  {formatDateTime(window.startsAt)} – {formatDateTime(window.endsAt)}
                </span>
                {window.reason && <span className="ml-2 text-gray-500">{window.reason}</span>}
              </div>
              <button
                type="button"
                disabled={busy}
                onClick={() => handleCancel(window.id)}
                className="text-red-700 hover:underline disabled:opacity-50"
              >
                Cancel
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
