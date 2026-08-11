import { get } from './client'
import type { MetricsWindow, ServiceMetricsResponse } from '../types'

export function getServiceMetrics(
  serviceId: string,
  window: MetricsWindow = '24h',
): Promise<ServiceMetricsResponse> {
  return get<ServiceMetricsResponse>(
    `/services/${serviceId}/metrics?window=${encodeURIComponent(window)}`,
  )
}
