import { get } from './client'
import type { StatusEventResponse } from '../types'

// GET /api/services/{id}/status-events -> StatusEventController.listForService (newest first)
export function getStatusEvents(serviceId: string): Promise<StatusEventResponse[]> {
  return get<StatusEventResponse[]>(`/services/${serviceId}/status-events`)
}
