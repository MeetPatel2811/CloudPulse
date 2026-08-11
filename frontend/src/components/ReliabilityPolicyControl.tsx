import { useEffect, useState } from 'react'
import { getReliabilityPolicy, updateReliabilityPolicy } from '../api/reliabilityPolicy'
import type { MonitoredService, ReliabilityPolicyResponse } from '../types'

const MIN_THRESHOLD_MS = 100
const MAX_THRESHOLD_MS = 5_000

export function ReliabilityPolicyControl({
  service,
  onChanged,
}: {
  service: MonitoredService
  onChanged: () => void | Promise<void>
}) {
  const [settings, setSettings] = useState<ReliabilityPolicyResponse | null>(null)
  const [useCustomThreshold, setUseCustomThreshold] = useState(false)
  const [thresholdInput, setThresholdInput] = useState('')
  const [failureThresholdInput, setFailureThresholdInput] = useState(1)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    getReliabilityPolicy(service.id)
      .then((nextSettings) => {
        if (cancelled) return
        setSettings(nextSettings)
        setUseCustomThreshold(nextSettings.latencyThresholdMs !== null)
        setThresholdInput(String(nextSettings.latencyThresholdMs ?? nextSettings.effectiveLatencyThresholdMs))
        setFailureThresholdInput(nextSettings.alertFailureThreshold)
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setError(caught instanceof Error ? caught.message : 'Could not load the reliability settings')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [
    service.id,
    service.activeEvaluationStrategy,
    service.alertFailureThreshold,
    service.consecutiveFailureCount,
  ])

  async function saveSettings() {
    if (!settings) return

    let nextThreshold: number | null = null
    if (useCustomThreshold) {
      nextThreshold = Number(thresholdInput)
      if (!Number.isInteger(nextThreshold)
          || nextThreshold < MIN_THRESHOLD_MS
          || nextThreshold > MAX_THRESHOLD_MS) {
        setError(`Enter a whole number from ${MIN_THRESHOLD_MS} to ${MAX_THRESHOLD_MS} ms.`)
        return
      }
    }

    setBusy(true)
    setError(null)
    setConfirmation(null)
    try {
      const updated = await updateReliabilityPolicy(
        service.id,
        nextThreshold,
        failureThresholdInput,
      )
      setSettings(updated)
      setUseCustomThreshold(updated.latencyThresholdMs !== null)
      setThresholdInput(String(updated.latencyThresholdMs ?? updated.effectiveLatencyThresholdMs))
      setFailureThresholdInput(updated.alertFailureThreshold)
      setConfirmation(
        `Reliability settings saved. Alerts open after ${updated.alertFailureThreshold} ${updated.alertFailureThreshold === 1 ? 'unhealthy check' : 'unhealthy checks'}.`,
      )
      await onChanged()
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not save the reliability settings')
    } finally {
      setBusy(false)
    }
  }

  const effectiveThreshold = settings?.effectiveLatencyThresholdMs
  const failureProgress = settings
    ? Math.min(settings.consecutiveFailureCount, settings.alertFailureThreshold)
    : 0

  return (
    <section
      aria-busy={loading || busy}
      className="mt-3 rounded-md border border-blue-100 bg-blue-50/60 p-3"
      aria-labelledby={`latency-threshold-${service.id}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 id={`latency-threshold-${service.id}`} className="text-sm font-semibold text-gray-900">
            Latency threshold
          </h3>
          <p className="mt-0.5 text-xs leading-5 text-gray-600">
            Responses slower than this become degraded.
          </p>
        </div>
        <div className="shrink-0 rounded-md bg-white px-2.5 py-1.5 text-right shadow-sm ring-1 ring-blue-100">
          <span className="block text-[10px] font-semibold uppercase tracking-wide text-blue-600">Effective</span>
          <strong className="font-mono text-sm text-gray-900">
            {effectiveThreshold === undefined ? '—' : `${effectiveThreshold} ms`}
          </strong>
        </div>
      </div>

      {loading ? (
        <p className="mt-3 text-xs text-gray-600" role="status">Loading threshold…</p>
      ) : settings ? (
        <>
          <div className="mt-3 grid grid-cols-2 gap-1 rounded-md bg-white p-1 ring-1 ring-gray-200" aria-label="Threshold source">
            <button
              type="button"
              aria-pressed={!useCustomThreshold}
              disabled={busy}
              onClick={() => {
                setUseCustomThreshold(false)
                setThresholdInput(String(settings.effectiveLatencyThresholdMs))
                setError(null)
              }}
              className={`rounded px-2 py-1.5 text-xs font-semibold ${!useCustomThreshold ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-50'}`}
            >
              Policy default
            </button>
            <button
              type="button"
              aria-pressed={useCustomThreshold}
              disabled={busy}
              onClick={() => {
                setUseCustomThreshold(true)
                setThresholdInput(String(settings.latencyThresholdMs ?? settings.effectiveLatencyThresholdMs))
                setError(null)
              }}
              className={`rounded px-2 py-1.5 text-xs font-semibold ${useCustomThreshold ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-50'}`}
            >
              Custom
            </button>
          </div>

          {useCustomThreshold && (
            <div className="mt-3">
              <label htmlFor={`latency-input-${service.id}`} className="text-xs font-medium text-gray-700">
                Threshold in milliseconds
              </label>
              <div className="mt-1 flex items-center gap-2">
                <input
                  id={`latency-input-${service.id}`}
                  type="number"
                  min={MIN_THRESHOLD_MS}
                  max={MAX_THRESHOLD_MS}
                  step={100}
                  value={thresholdInput}
                  disabled={busy}
                  onChange={(event) => setThresholdInput(event.target.value)}
                  className="min-w-0 flex-1 rounded-md border border-gray-300 bg-white px-2.5 py-1.5 text-sm text-gray-900 focus:border-blue-500"
                />
                <span className="text-xs font-medium text-gray-500">ms</span>
              </div>
            </div>
          )}

          <p className="mt-2 text-[11px] leading-4 text-gray-500">
            {!useCustomThreshold
              ? `${settings.evaluationPolicy} currently supplies the threshold.`
              : 'This override applies on the next health check.'}
          </p>

          <div className="mt-3 border-t border-blue-100 pt-3">
            <div className="flex items-center justify-between gap-3">
              <div>
                <label htmlFor={`failure-threshold-${service.id}`} className="text-xs font-semibold text-gray-800">
                  Alert trigger
                </label>
                <p className="mt-0.5 text-[11px] leading-4 text-gray-500">Avoid alerts from one-off failures.</p>
              </div>
              <select
                id={`failure-threshold-${service.id}`}
                value={failureThresholdInput}
                disabled={busy}
                onChange={(event) => setFailureThresholdInput(Number(event.target.value))}
                className="rounded-md border border-gray-300 bg-white px-2 py-1.5 text-xs font-semibold text-gray-800"
              >
                {[1, 2, 3, 4, 5].map((count) => (
                  <option key={count} value={count}>After {count}</option>
                ))}
              </select>
            </div>

            <div className="mt-3 rounded-md bg-white p-2.5 ring-1 ring-blue-100">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-gray-600">Current unhealthy streak</span>
                <strong className="font-mono text-gray-900">
                  {settings.consecutiveFailureCount} / {settings.alertFailureThreshold}
                </strong>
              </div>
              <progress
                aria-label="Unhealthy check progress toward alert"
                className="mt-2 h-1.5 w-full accent-rose-500"
                max={settings.alertFailureThreshold}
                value={failureProgress}
              />
              <p className="mt-1 text-[11px] leading-4 text-gray-500">
                {settings.consecutiveFailureCount === 0
                  ? 'No unhealthy checks are pending.'
                  : settings.consecutiveFailureCount < settings.alertFailureThreshold
                    ? `${settings.alertFailureThreshold - settings.consecutiveFailureCount} more before an alert opens.`
                    : 'The alert threshold has been reached.'}
              </p>
            </div>
          </div>

          <button
            type="button"
            disabled={busy}
            onClick={saveSettings}
            className="mt-3 w-full rounded-md border border-blue-200 bg-white px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-50 disabled:opacity-50"
          >
            {busy ? 'Saving…' : 'Save reliability settings'}
          </button>
        </>
      ) : null}

      {confirmation && <p role="status" aria-live="polite" className="mt-2 text-xs text-green-700">{confirmation}</p>}
      {error && <p role="alert" className="mt-2 text-xs text-red-700">{error}</p>}
    </section>
  )
}
