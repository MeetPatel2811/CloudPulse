import { useEffect, useState } from 'react'
import { getHealthChecks } from '../api/healthChecks'
import { getServiceMetrics } from '../api/metrics'
import type {
  HealthCheckResultResponse,
  MetricsWindow,
  MonitoredService,
  ServiceMetricsResponse,
} from '../types'

export function useFleetInsights(
  services: MonitoredService[] | null,
  window: MetricsWindow,
  refreshVersion: number,
) {
  const [metrics, setMetrics] = useState<Record<string, ServiceMetricsResponse>>({})
  const [histories, setHistories] = useState<Record<string, HealthCheckResultResponse[]>>({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!services || services.length === 0) {
      setMetrics({})
      setHistories({})
      return
    }

    let cancelled = false
    setLoading(true)
    Promise.all(
      services.map(async (service) => {
        const [serviceMetrics, history] = await Promise.all([
          getServiceMetrics(service.id, window),
          getHealthChecks(service.id, { limit: 40 }),
        ])
        return { id: service.id, serviceMetrics, history }
      }),
    )
      .then((results) => {
        if (cancelled) return
        setMetrics(Object.fromEntries(results.map((result) => [result.id, result.serviceMetrics])))
        setHistories(Object.fromEntries(results.map((result) => [result.id, result.history])))
      })
      .catch(() => {
        if (cancelled) return
        setMetrics({})
        setHistories({})
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [services, window, refreshVersion])

  return { metrics, histories, loading }
}
