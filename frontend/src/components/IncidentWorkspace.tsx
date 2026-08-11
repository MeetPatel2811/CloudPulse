import { useEffect, useId, useState } from 'react'
import { addIncidentNote, assignIncidentOwner, getIncident } from '../api/incidents'
import { useDialogFocus } from '../hooks/useDialogFocus'
import type { AlertResponse, IncidentActivityType, IncidentResponse } from '../types'
import { formatDateTime } from '../lib/format'
import { Icon } from './Icon'

const ACTIVITY_LABELS: Record<IncidentActivityType, string> = {
  ACKNOWLEDGED: 'Acknowledged',
  ASSIGNED: 'Owner assigned',
  NOTE: 'Note added',
  RESOLVED: 'Resolved',
}

export function IncidentWorkspace({
  alert,
  onClose,
  onSelectService,
}: {
  alert: AlertResponse
  onClose: () => void
  onSelectService: (serviceId: string) => void
}) {
  const titleId = useId()
  const dialogRef = useDialogFocus(onClose)
  const [incident, setIncident] = useState<IncidentResponse | null>(null)
  const [owner, setOwner] = useState('')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<'owner' | 'note' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getIncident(alert.id)
      .then((response) => {
        if (cancelled) return
        setIncident(response)
        setOwner(response.owner ?? '')
      })
      .catch((caught: unknown) => {
        if (!cancelled) setError(caught instanceof Error ? caught.message : 'Could not load the incident workspace')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [alert.id])

  async function saveOwner(event: React.FormEvent) {
    event.preventDefault()
    const nextOwner = owner.trim()
    if (!nextOwner) return
    setBusy('owner')
    setError(null)
    setMessage(null)
    try {
      const response = await assignIncidentOwner(alert.id, nextOwner)
      setIncident(response)
      setOwner(response.owner ?? '')
      setMessage(`Incident assigned to ${response.owner}.`)
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not assign the incident')
    } finally {
      setBusy(null)
    }
  }

  async function saveNote(event: React.FormEvent) {
    event.preventDefault()
    const nextNote = note.trim()
    if (!nextNote) return
    setBusy('note')
    setError(null)
    setMessage(null)
    try {
      const response = await addIncidentNote(alert.id, nextNote)
      setIncident(response)
      setNote('')
      setMessage('Note added to the activity timeline.')
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not add the incident note')
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="dialog-backdrop">
      <section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-busy={loading || busy !== null}
        tabIndex={-1}
        className="incident-workspace"
      >
        <header className="incident-workspace__header">
          <div>
            <p className="eyebrow">Incident workspace</p>
            <h2 id={titleId}>{alert.serviceName ?? 'Unknown service'}</h2>
            <p>{alert.message}</p>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close incident workspace">
            <Icon name="close" className="h-5 w-5" />
          </button>
        </header>

        <div className="incident-workspace__meta">
          <span className={`incident-status incident-status--${alert.status.toLowerCase()}`}>{alert.status.toLowerCase()}</span>
          <span>{alert.severity.toLowerCase()} severity</span>
          <span>Raised {formatDateTime(alert.createdAt)}</span>
          {alert.serviceId && (
            <button type="button" onClick={() => onSelectService(alert.serviceId!)}>Inspect service</button>
          )}
        </div>

        {error && <div className="page-error" role="alert">{error}</div>}
        {message && <div className="incident-workspace__success" role="status" aria-live="polite">{message}</div>}

        {loading ? (
          <div className="incident-workspace__loading" role="status">Loading ownership and activity…</div>
        ) : (
          <div className="incident-workspace__grid">
            <div className="incident-workspace__controls">
              <section aria-labelledby={`${titleId}-owner`}>
                <div className="incident-workspace__section-heading">
                  <div><p className="eyebrow">Accountability</p><h3 id={`${titleId}-owner`}>Incident owner</h3></div>
                  <span>{incident?.owner ?? 'Unassigned'}</span>
                </div>
                <form onSubmit={saveOwner}>
                  <label htmlFor={`${titleId}-owner-input`}>Assign or reassign owner</label>
                  <div className="incident-inline-form">
                    <input
                      id={`${titleId}-owner-input`}
                      data-autofocus
                      type="text"
                      maxLength={255}
                      autoComplete="name"
                      value={owner}
                      disabled={busy !== null}
                      onChange={(event) => setOwner(event.target.value)}
                      placeholder="Operator name"
                      required
                    />
                    <button type="submit" className="button button--secondary" disabled={!owner.trim() || busy !== null}>
                      {busy === 'owner' ? 'Assigning…' : 'Assign'}
                    </button>
                  </div>
                </form>
              </section>

              <section aria-labelledby={`${titleId}-note`}>
                <div className="incident-workspace__section-heading">
                  <div><p className="eyebrow">Collaboration</p><h3 id={`${titleId}-note`}>Add note</h3></div>
                </div>
                <form onSubmit={saveNote}>
                  <label htmlFor={`${titleId}-note-input`}>Timeline note</label>
                  <textarea
                    id={`${titleId}-note-input`}
                    maxLength={2000}
                    rows={4}
                    value={note}
                    disabled={busy !== null}
                    onChange={(event) => setNote(event.target.value)}
                    placeholder="Share investigation progress or the next action…"
                    required
                  />
                  <div className="incident-note-footer">
                    <span>{note.length}/2000</span>
                    <button type="submit" className="button button--primary" disabled={!note.trim() || busy !== null}>
                      {busy === 'note' ? 'Adding…' : 'Add note'}
                    </button>
                  </div>
                </form>
              </section>
            </div>

            <section className="incident-activity" aria-labelledby={`${titleId}-activity`}>
              <div className="incident-workspace__section-heading">
                <div><p className="eyebrow">Audit trail</p><h3 id={`${titleId}-activity`}>Activity timeline</h3></div>
                <span>{incident?.activity.length ?? 0} entries</span>
              </div>
              {incident?.activity.length ? (
                <ol>
                  {[...incident.activity].reverse().map((activity) => (
                    <li key={activity.id}>
                      <span className={`activity-marker activity-marker--${activity.type.toLowerCase()}`} aria-hidden="true" />
                      <div>
                        <div><strong>{ACTIVITY_LABELS[activity.type]}</strong><time dateTime={activity.createdAt}>{formatDateTime(activity.createdAt)}</time></div>
                        <p>{activity.detail}</p>
                        <small>by {activity.author}</small>
                      </div>
                    </li>
                  ))}
                </ol>
              ) : (
                <div className="quiet-state"><Icon name="clock" className="h-5 w-5" /><span>No activity has been recorded yet.</span></div>
              )}
            </section>
          </div>
        )}
      </section>
    </div>
  )
}
