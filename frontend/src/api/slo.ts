import { put } from './client'
import type { MonitoredService } from '../types'

export function updateSloTarget(
  serviceId: string,
  availabilitySloPercent: number,
): Promise<MonitoredService> {
  return put<MonitoredService>(`/services/${serviceId}/slo`, { availabilitySloPercent })
}

