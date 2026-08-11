import { useId, useState } from 'react'
import { useDialogFocus } from '../hooks/useDialogFocus'
import { parseTags } from '../lib/serviceMetadata'
import type { CheckIntervalSeconds, MonitorType, RegisterServiceRequest } from '../types'

interface ServiceFormProps {
  title: string
  submitLabel: string
  initial?: RegisterServiceRequest
  onSubmit: (request: RegisterServiceRequest) => Promise<void>
  onClose: () => void
}

const MONITOR_TYPES: MonitorType[] = ['HTTP', 'MOCK', 'KEYWORD']

const CHECK_INTERVALS: { value: CheckIntervalSeconds; label: string }[] = [
  { value: 15, label: '15 seconds' },
  { value: 30, label: '30 seconds' },
  { value: 60, label: '1 minute' },
  { value: 300, label: '5 minutes' },
]

const inputClassName =
  'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-900 focus:border-blue-500'
const labelClassName = 'mb-1 block text-sm font-medium text-gray-700'

export function ServiceForm({ title, submitLabel, initial, onSubmit, onClose }: ServiceFormProps) {
  const titleId = useId()
  const errorId = useId()
  const dialogRef = useDialogFocus(onClose)
  const [name, setName] = useState(initial?.name ?? '')
  const [healthUrl, setHealthUrl] = useState(initial?.healthUrl ?? '')
  const [monitorType, setMonitorType] = useState<MonitorType>(initial?.monitorType ?? 'HTTP')
  const [checkIntervalSeconds, setCheckIntervalSeconds] = useState<CheckIntervalSeconds>(
    initial?.checkIntervalSeconds ?? 15,
  )
  const [expectedKeyword, setExpectedKeyword] = useState(initial?.expectedKeyword ?? '')
  const [serviceGroup, setServiceGroup] = useState(initial?.serviceGroup ?? '')
  const [tags, setTags] = useState(initial?.tags.join(', ') ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit({
        name: name.trim(),
        healthUrl: healthUrl.trim(),
        monitorType,
        checkIntervalSeconds,
        ...(monitorType === 'KEYWORD' ? { expectedKeyword } : {}),
        serviceGroup: serviceGroup.trim() || null,
        tags: parseTags(tags),
      })
      // On success the parent closes this form; nothing more to do here.
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
      setSubmitting(false)
    }
  }

  return (
    <div className="dialog-backdrop">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={error ? errorId : undefined}
        aria-busy={submitting}
        tabIndex={-1}
        className="compact-dialog"
      >
        <h3 id={titleId} className="mb-4 text-lg font-semibold text-gray-900">{title}</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="service-name" className={labelClassName}>Name</label>
            <input
              id="service-name"
              type="text"
              autoComplete="off"
              data-autofocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className={inputClassName}
            />
          </div>

          <div>
            <label htmlFor="service-health-url" className={labelClassName}>Health URL</label>
            <input
              id="service-health-url"
              type="url"
              autoComplete="url"
              value={healthUrl}
              onChange={(e) => setHealthUrl(e.target.value)}
              required
              placeholder="http://localhost:8081/health"
              className={inputClassName}
            />
          </div>

          <div>
            <label htmlFor="service-monitor-type" className={labelClassName}>Monitor type</label>
            <select
              id="service-monitor-type"
              value={monitorType}
              onChange={(e) => setMonitorType(e.target.value as MonitorType)}
              className={inputClassName}
            >
              {MONITOR_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>

          {monitorType === 'KEYWORD' && (
            <div>
              <label htmlFor="service-expected-keyword" className={labelClassName}>Expected keyword</label>
              <input
                id="service-expected-keyword"
                type="text"
                value={expectedKeyword}
                onChange={(e) => setExpectedKeyword(e.target.value)}
                required
                placeholder="All Systems Operational"
                className={inputClassName}
              />
            </div>
          )}

          <div>
            <label htmlFor="service-check-interval" className={labelClassName}>Check interval</label>
            <select
              id="service-check-interval"
              value={checkIntervalSeconds}
              onChange={(e) => setCheckIntervalSeconds(Number(e.target.value) as CheckIntervalSeconds)}
              className={inputClassName}
            >
              {CHECK_INTERVALS.map(({ value, label }) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="service-group" className={labelClassName}>Service group <span className="font-normal text-gray-500">(optional)</span></label>
            <input
              id="service-group"
              type="text"
              autoComplete="off"
              maxLength={80}
              value={serviceGroup}
              onChange={(event) => setServiceGroup(event.target.value)}
              placeholder="Platform"
              className={inputClassName}
            />
          </div>

          <div>
            <label htmlFor="service-tags" className={labelClassName}>Tags <span className="font-normal text-gray-500">(optional)</span></label>
            <input
              id="service-tags"
              type="text"
              autoComplete="off"
              value={tags}
              onChange={(event) => setTags(event.target.value)}
              placeholder="api, critical, customer-facing"
              aria-describedby="service-tags-help"
              className={inputClassName}
            />
            <p id="service-tags-help" className="mt-1 text-xs text-gray-500">Separate up to 10 tags with commas.</p>
          </div>

          {error && <p id={errorId} role="alert" className="text-sm text-red-700">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? 'Saving…' : submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
