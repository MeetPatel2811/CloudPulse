import { useEffect, useState } from 'react'
import { updateSloTarget } from '../api/slo'
import type { MonitoredService } from '../types'

const SLO_OPTIONS = [95, 99, 99.5, 99.9, 99.99]

export function SloTargetControl({
  service,
  onChanged,
}: {
  service: MonitoredService
  onChanged: () => void | Promise<void>
}) {
  const [target, setTarget] = useState(service.availabilitySloPercent)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => setTarget(service.availabilitySloPercent), [service.availabilitySloPercent])

  async function saveTarget() {
    setBusy(true)
    setMessage(null)
    setError(null)
    try {
      const updated = await updateSloTarget(service.id, target)
      setTarget(updated.availabilitySloPercent)
      setMessage(`${updated.availabilitySloPercent}% availability objective saved.`)
      await onChanged()
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not save the SLO target')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="slo-control" aria-labelledby={`slo-target-${service.id}`} aria-busy={busy}>
      <div>
        <h3 id={`slo-target-${service.id}`}>Availability SLO</h3>
        <p>Target used to calculate this service’s error budget.</p>
      </div>
      <div className="slo-control__form">
        <label htmlFor={`slo-select-${service.id}`}>Target</label>
        <select
          id={`slo-select-${service.id}`}
          value={target}
          disabled={busy}
          onChange={(event) => {
            setTarget(Number(event.target.value))
            setMessage(null)
          }}
        >
          {SLO_OPTIONS.map((option) => <option key={option} value={option}>{option}%</option>)}
        </select>
        <button type="button" disabled={busy || target === service.availabilitySloPercent} onClick={saveTarget}>
          {busy ? 'Saving…' : 'Save SLO'}
        </button>
      </div>
      {message && <p className="slo-control__success" role="status">{message}</p>}
      {error && <p className="slo-control__error" role="alert">{error}</p>}
    </section>
  )
}

