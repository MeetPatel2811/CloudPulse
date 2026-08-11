import { useState } from 'react'
import { disableService, enableService, runCheck } from '../api/services'
import type { MonitoredService } from '../types'

// Manual operator actions for one service, wired to the backend Command endpoints:
// run a check now, and turn scheduled monitoring on or off.
export function ServiceControls({
  service,
  onChanged,
}: {
  service: MonitoredService
  onChanged: () => void | Promise<void>
}) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    setBusy(true)
    try {
      await action()
      await onChanged()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'The action could not be completed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div role="group" aria-label={`Controls for ${service.name}`} aria-busy={busy} className="space-y-2">
      {error && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-sm text-red-700">
          {error}
        </div>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          disabled={busy}
          onClick={() => run(() => runCheck(service.id))}
          className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          Run check now
        </button>
        {service.enabled ? (
          <button
            type="button"
            disabled={busy}
            onClick={() => run(() => disableService(service.id))}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Disable monitoring
          </button>
        ) : (
          <button
            type="button"
            disabled={busy}
            onClick={() => run(() => enableService(service.id))}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Enable monitoring
          </button>
        )}
        <span role="status" aria-live="polite" className="text-xs font-medium text-gray-600">
          {busy ? 'Updating service…' : service.enabled ? 'Monitoring on' : 'Monitoring off'}
        </span>
      </div>
    </div>
  )
}
