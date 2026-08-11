import { useEffect, useState } from 'react'
import { updateEvaluationPolicy } from '../api/evaluationPolicy'
import type { EvaluationPolicy, MonitoredService } from '../types'

export function EvaluationPolicyControl({
  service,
  onChanged,
}: {
  service: MonitoredService
  onChanged: () => void | Promise<void>
}) {
  const [policy, setPolicy] = useState<EvaluationPolicy>(service.activeEvaluationStrategy)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState<string | null>(null)
  const controlId = `evaluation-policy-${service.id}`
  const descriptionId = `${controlId}-description`

  useEffect(() => {
    setPolicy(service.activeEvaluationStrategy)
    setError(null)
    setConfirmation(null)
  }, [service.id])

  // Keep the control synchronized with refreshes without clearing a successful
  // save message when the parent fetches the newly persisted policy.
  useEffect(() => {
    setPolicy(service.activeEvaluationStrategy)
  }, [service.activeEvaluationStrategy])

  async function handlePolicyChange(nextPolicy: EvaluationPolicy) {
    if (nextPolicy === policy) return

    const previousPolicy = policy
    setPolicy(nextPolicy)
    setBusy(true)
    setError(null)
    setConfirmation(null)

    try {
      const updated = await updateEvaluationPolicy(service.id, nextPolicy)
      setPolicy(updated.evaluationPolicy)
      setConfirmation(`${updated.evaluationPolicy} policy saved. It will apply on the next health check.`)
      await onChanged()
    } catch (err: unknown) {
      setPolicy(previousPolicy)
      setError(err instanceof Error ? err.message : 'Could not update the evaluation policy')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div aria-busy={busy} className="rounded-md border border-gray-200 bg-gray-50 p-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <label htmlFor={controlId} className="text-sm font-medium text-gray-800">
            Evaluation policy
          </label>
          <p id={descriptionId} className="text-xs text-gray-500">
            Normal tolerates moderate latency; Strict marks slow responses degraded sooner.
          </p>
        </div>
        <select
          id={controlId}
          value={policy}
          disabled={busy}
          aria-describedby={descriptionId}
          onChange={(event) => handlePolicyChange(event.target.value as EvaluationPolicy)}
          className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-800 disabled:opacity-50"
        >
          <option value="NORMAL">Normal</option>
          <option value="STRICT">Strict</option>
        </select>
      </div>

      {busy && <p role="status" aria-live="polite" className="mt-2 text-xs text-gray-600">Saving evaluation policy…</p>}
      {confirmation && <p role="status" aria-live="polite" className="mt-2 text-xs text-green-700">{confirmation}</p>}
      {error && <p role="alert" className="mt-2 text-xs text-red-700">Could not update policy: {error}</p>}
    </div>
  )
}
