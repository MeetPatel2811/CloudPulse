import { get } from './client'
import type { HealthCheckResultResponse } from '../types'

// GET /api/services/{id}/health-checks -> HealthCheckHistoryController.listForService
interface HealthCheckQuery {
  from?: string
  to?: string
  limit?: number
}

export function getHealthChecks(
  serviceId: string,
  query: HealthCheckQuery = {},
): Promise<HealthCheckResultResponse[]> {
  const params = new URLSearchParams()
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  if (query.limit !== undefined) params.set('limit', String(query.limit))
  const suffix = params.size > 0 ? `?${params.toString()}` : ''
  return get<HealthCheckResultResponse[]>(`/services/${serviceId}/health-checks${suffix}`)
}
