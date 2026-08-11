import { useId, useMemo, useState } from 'react'
import { probeService, registerService } from '../api/services'
import { useDialogFocus } from '../hooks/useDialogFocus'
import type { CheckIntervalSeconds, MonitoredService, ProbeServiceResponse } from '../types'
import { formatLatency } from '../lib/format'
import { parseTags } from '../lib/serviceMetadata'
import { Icon } from './Icon'

const CHECK_INTERVALS: { value: CheckIntervalSeconds; label: string }[] = [
  { value: 15, label: '15 seconds' },
  { value: 30, label: '30 seconds' },
  { value: 60, label: '1 minute' },
  { value: 300, label: '5 minutes' },
]

function normalizeUrl(value: string): string {
  const trimmed = value.trim()
  if (!trimmed || /^https?:\/\//i.test(trimmed)) return trimmed
  return `https://${trimmed}`
}

function modeLabel(mode: ProbeServiceResponse['detectedMode']): string {
  if (mode === 'STANDARD_JSON') return 'CloudPulse health JSON'
  if (mode === 'LEGACY_JSON') return 'Legacy health JSON'
  if (mode === 'GENERIC_HTTP') return 'Website / generic HTTP'
  return 'Unknown response'
}

export function AddMonitorWizard({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: (service: MonitoredService) => void | Promise<void>
}) {
  const titleId = useId()
  const descriptionId = useId()
  const dialogRef = useDialogFocus(onClose)
  const [name, setName] = useState('')
  const [healthUrl, setHealthUrl] = useState('')
  const [serviceGroup, setServiceGroup] = useState('')
  const [tags, setTags] = useState('')
  const [probedUrl, setProbedUrl] = useState<string | null>(null)
  const [probe, setProbe] = useState<ProbeServiceResponse | null>(null)
  const [probing, setProbing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [checkIntervalSeconds, setCheckIntervalSeconds] = useState<CheckIntervalSeconds>(15)

  const normalizedUrl = useMemo(() => normalizeUrl(healthUrl), [healthUrl])
  const verified = Boolean(probe?.reachable && probedUrl === normalizedUrl)

  function changeUrl(value: string) {
    setHealthUrl(value)
    setProbe(null)
    setProbedUrl(null)
    setError(null)
  }

  async function handleProbe() {
    if (!normalizedUrl) return
    setProbing(true)
    setError(null)
    setProbe(null)
    try {
      const result = await probeService(normalizedUrl)
      setHealthUrl(normalizedUrl)
      setProbedUrl(normalizedUrl)
      setProbe(result)
      if (!result.reachable) {
        setError(result.message || 'CloudPulse could not reach this endpoint')
      }
      if (!name.trim()) {
        try {
          const hostname = new URL(normalizedUrl).hostname.replace(/^www\./, '')
          setName(hostname.split('.')[0].replace(/(^|-)(\w)/g, (_, prefix, letter) => `${prefix}${letter.toUpperCase()}`))
        } catch {
          // The backend returns the useful malformed-URL message.
        }
      }
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'CloudPulse could not test this endpoint')
    } finally {
      setProbing(false)
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!verified) return
    setSaving(true)
    setError(null)
    try {
      const service = await registerService({
        name: name.trim(),
        healthUrl: normalizedUrl,
        monitorType: 'HTTP',
        checkIntervalSeconds,
        serviceGroup: serviceGroup.trim() || null,
        tags: parseTags(tags),
      })
      await onCreated(service)
      onClose()
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'CloudPulse could not create this monitor')
      setSaving(false)
    }
  }

  return (
    <div className="dialog-backdrop">
      <section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        aria-busy={probing || saving}
        tabIndex={-1}
        className="monitor-wizard"
      >
        <header className="monitor-wizard__header">
          <div>
            <p className="eyebrow">New monitor</p>
            <h2 id={titleId}>Start watching an endpoint</h2>
            <p id={descriptionId}>CloudPulse verifies the target before it saves anything.</p>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close add monitor dialog">
            <Icon name="close" className="h-5 w-5" />
          </button>
        </header>

        <ol className="wizard-steps" aria-label="Monitor setup progress">
          <li className="wizard-steps__active"><span>1</span>Target</li>
          <li className={probe ? 'wizard-steps__active' : ''}><span>2</span>Verify</li>
          <li className={verified ? 'wizard-steps__active' : ''}><span>3</span>Monitor</li>
        </ol>

        <form onSubmit={handleSubmit} className="monitor-wizard__body">
          <div className="form-field">
            <label htmlFor="monitor-url">Website or health endpoint</label>
            <div className="url-probe-control">
              <span aria-hidden="true"><Icon name="globe" className="h-5 w-5" /></span>
              <input
                id="monitor-url"
                data-autofocus
                type="text"
                inputMode="url"
                autoComplete="url"
                value={healthUrl}
                onChange={(event) => changeUrl(event.target.value)}
                placeholder="notion.com or https://api.example.com/health"
                required
              />
              <button type="button" onClick={handleProbe} disabled={probing || !healthUrl.trim()}>
                {probing ? 'Testing…' : 'Test connection'}
              </button>
            </div>
            <p>Public HTTP and HTTPS targets only. Redirects and destination addresses are validated safely.</p>
          </div>

          {probe && (
            <div className={`probe-result ${probe.reachable ? 'probe-result--success' : 'probe-result--failure'}`} role="status">
              <span className="probe-result__icon">
                <Icon name={probe.reachable ? 'check' : 'incidents'} className="h-5 w-5" />
              </span>
              <div>
                <strong>{probe.reachable ? 'Connection verified' : 'Target did not pass verification'}</strong>
                <p>{probe.message}</p>
                <dl>
                  <div><dt>HTTP</dt><dd>{probe.httpStatus || 'No response'}</dd></div>
                  <div><dt>Latency</dt><dd>{formatLatency(probe.reachable ? probe.responseTimeMs : null)}</dd></div>
                  <div><dt>Detected</dt><dd>{modeLabel(probe.detectedMode)}</dd></div>
                  <div><dt>Redirects</dt><dd>{probe.redirectCount}</dd></div>
                </dl>
              </div>
            </div>
          )}

          <div className="form-field">
            <label htmlFor="monitor-name">Monitor name</label>
            <input
              id="monitor-name"
              type="text"
              autoComplete="off"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Notion website"
              required
            />
            <p>Use the name your team will recognize during an incident.</p>
          </div>

          <div className="form-field">
            <label htmlFor="monitor-check-interval">Check interval</label>
            <select
              id="monitor-check-interval"
              value={checkIntervalSeconds}
              onChange={(event) => setCheckIntervalSeconds(Number(event.target.value) as CheckIntervalSeconds)}
            >
              {CHECK_INTERVALS.map(({ value, label }) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
            <p>How often CloudPulse checks this endpoint. Change it later from the monitor's edit dialog.</p>
          </div>

          <div className="monitor-metadata-grid">
            <div className="form-field">
              <label htmlFor="monitor-group">Service group <span>(optional)</span></label>
              <input
                id="monitor-group"
                type="text"
                autoComplete="off"
                maxLength={80}
                value={serviceGroup}
                onChange={(event) => setServiceGroup(event.target.value)}
                placeholder="Platform"
              />
              <p>Group related services for faster reporting.</p>
            </div>
            <div className="form-field">
              <label htmlFor="monitor-tags">Tags <span>(optional)</span></label>
              <input
                id="monitor-tags"
                type="text"
                autoComplete="off"
                value={tags}
                onChange={(event) => setTags(event.target.value)}
                placeholder="api, critical"
              />
              <p>Comma-separated, up to 10 tags.</p>
            </div>
          </div>

          <div className="monitor-summary">
            <Icon name="shield" className="h-5 w-5" />
            <div>
              <strong>Safe HTTP monitoring</strong>
              <p>
                Checks run every {CHECK_INTERVALS.find((i) => i.value === checkIntervalSeconds)?.label ?? '15 seconds'}.
                Evaluation starts in Normal mode and can be tightened later.
              </p>
            </div>
          </div>

          {error && <div className="form-error" role="alert">{error}</div>}

          <footer className="monitor-wizard__footer">
            <button type="button" className="button button--secondary" onClick={onClose}>Cancel</button>
            <button
              type="submit"
              className="button button--primary"
              disabled={!verified || !name.trim() || saving}
              title={!verified ? 'Test the connection before adding this monitor' : undefined}
            >
              {saving ? 'Adding monitor…' : 'Add monitor'}
              {!saving && <Icon name="arrow-right" className="h-4 w-4" />}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
