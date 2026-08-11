import { useEffect, useState } from 'react'
import {
  deleteWebhook,
  getNotificationDeliveries,
  getWebhookTargets,
  registerWebhook,
} from '../api/webhooks'
import type {
  NotificationDeliveryResponse,
  WebhookProvider,
  WebhookTargetResponse,
} from '../types'
import { formatDateTime } from '../lib/format'
import { ConfirmDialog } from './ConfirmDialog'
import { Icon } from './Icon'

const PROVIDERS: WebhookProvider[] = ['SLACK', 'TEAMS', 'DISCORD']

function maskWebhookUrl(value: string): string {
  try {
    const url = new URL(value)
    return `${url.hostname}/••••••`
  } catch {
    return 'Protected webhook endpoint'
  }
}

export function NotificationCenter() {
  const [targets, setTargets] = useState<WebhookTargetResponse[]>([])
  const [deliveries, setDeliveries] = useState<NotificationDeliveryResponse[]>([])
  const [provider, setProvider] = useState<WebhookProvider>('SLACK')
  const [label, setLabel] = useState('')
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [deleting, setDeleting] = useState<WebhookTargetResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function load() {
    const [nextTargets, nextDeliveries] = await Promise.all([
      getWebhookTargets(),
      getNotificationDeliveries(),
    ])
    setTargets(nextTargets)
    setDeliveries(nextDeliveries)
  }

  useEffect(() => {
    let cancelled = false
    Promise.all([getWebhookTargets(), getNotificationDeliveries()])
      .then(([nextTargets, nextDeliveries]) => {
        if (cancelled) return
        setTargets(nextTargets)
        setDeliveries(nextDeliveries)
      })
      .catch((caught: unknown) => {
        if (!cancelled) setError(caught instanceof Error ? caught.message : 'Could not load notification settings')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function addTarget(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const created = await registerWebhook({ provider, url: url.trim(), label: label.trim() || undefined })
      setTargets((current) => [...current, created])
      setLabel('')
      setUrl('')
      setMessage(`${created.provider} destination added.`)
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not add the webhook destination')
    } finally {
      setBusy(false)
    }
  }

  async function removeTarget() {
    if (!deleting) return
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      await deleteWebhook(deleting.id)
      setTargets((current) => current.filter((target) => target.id !== deleting.id))
      setMessage(`${deleting.label || deleting.provider} destination removed.`)
      setDeleting(null)
    } catch (caught: unknown) {
      setDeleting(null)
      setError(caught instanceof Error ? caught.message : 'Could not remove the webhook destination')
    } finally {
      setBusy(false)
    }
  }

  async function refreshHistory() {
    setBusy(true)
    setError(null)
    try {
      await load()
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not refresh delivery history')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="notification-center" aria-busy={loading || busy}>
      {deleting && (
        <ConfirmDialog
          title="Remove notification destination"
          message={`Remove “${deleting.label || deleting.provider}”? Existing delivery history will remain available.`}
          confirmLabel="Remove destination"
          busy={busy}
          onConfirm={removeTarget}
          onCancel={() => setDeleting(null)}
        />
      )}

      {error && <div className="page-error" role="alert">{error}</div>}
      {message && <div className="notification-success" role="status" aria-live="polite">{message}</div>}

      <div className="notification-grid">
        <section className="panel notification-destinations" aria-labelledby="notification-destinations-title">
          <div className="panel__header">
            <div><p className="eyebrow">Outbound alerts</p><h2 id="notification-destinations-title">Webhook destinations</h2></div>
            <span className="panel__meta">{targets.length} configured</span>
          </div>

          <form className="webhook-form" onSubmit={addTarget}>
            <label>Provider
              <select value={provider} disabled={busy} onChange={(event) => setProvider(event.target.value as WebhookProvider)}>
                {PROVIDERS.map((value) => <option key={value} value={value}>{value.charAt(0) + value.slice(1).toLowerCase()}</option>)}
              </select>
            </label>
            <label>Label <span>(optional)</span>
              <input type="text" maxLength={255} value={label} disabled={busy} onChange={(event) => setLabel(event.target.value)} placeholder="Incident channel" />
            </label>
            <label className="webhook-form__url">Webhook URL
              <input type="url" value={url} disabled={busy} onChange={(event) => setUrl(event.target.value)} placeholder="https://hooks.example.com/…" required />
            </label>
            <button type="submit" className="button button--primary" disabled={busy || !url.trim()}>
              <Icon name="plus" className="h-4 w-4" /> {busy ? 'Adding…' : 'Add destination'}
            </button>
          </form>

          {loading ? (
            <div className="quiet-state" role="status">Loading destinations…</div>
          ) : targets.length === 0 ? (
            <div className="quiet-state"><Icon name="pulse" className="h-5 w-5" /><span>No webhook destinations configured.</span></div>
          ) : (
            <div className="webhook-target-list">
              {targets.map((target) => (
                <article key={target.id}>
                  <span className={`webhook-provider webhook-provider--${target.provider.toLowerCase()}`}>{target.provider.slice(0, 1)}</span>
                  <div><strong>{target.label || `${target.provider} destination`}</strong><span>{maskWebhookUrl(target.url)}</span><small>Added {formatDateTime(target.createdAt)}</small></div>
                  <span className={target.enabled ? 'target-enabled' : 'target-disabled'}>{target.enabled ? 'Active' : 'Disabled'}</span>
                  <button type="button" disabled={busy} onClick={() => setDeleting(target)}>Remove</button>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="panel delivery-history" aria-labelledby="delivery-history-title">
          <div className="panel__header">
            <div><p className="eyebrow">Audit</p><h2 id="delivery-history-title">Delivery history</h2></div>
            <button type="button" className="text-action" disabled={busy} onClick={refreshHistory}>
              <Icon name="refresh" className={`h-4 w-4 ${busy ? 'spin' : ''}`} /> Refresh
            </button>
          </div>
          {loading ? (
            <div className="quiet-state" role="status">Loading delivery history…</div>
          ) : deliveries.length === 0 ? (
            <div className="quiet-state"><Icon name="clock" className="h-5 w-5" /><span>No notifications have been delivered yet.</span></div>
          ) : (
            <div className="delivery-table-wrap">
              <table className="delivery-table">
                <caption className="sr-only">Webhook notification delivery history</caption>
                <thead><tr><th scope="col">Destination</th><th scope="col">Service</th><th scope="col">Result</th><th scope="col">Sent</th></tr></thead>
                <tbody>
                  {deliveries.map((delivery) => (
                    <tr key={delivery.id}>
                      <td><strong>{delivery.provider}</strong><span>{maskWebhookUrl(delivery.targetUrl)}</span></td>
                      <td><strong>{delivery.serviceName ?? 'Unknown service'}</strong><span title={delivery.message}>{delivery.message}</span></td>
                      <td><span className={`delivery-result delivery-result--${delivery.success ? 'success' : 'failure'}`}>{delivery.success ? 'Delivered' : 'Failed'}</span><small>{delivery.success ? `HTTP ${delivery.httpStatus}` : delivery.error || 'No response'}</small></td>
                      <td><time dateTime={delivery.sentAt}>{formatDateTime(delivery.sentAt)}</time></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
