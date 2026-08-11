import { get, post } from './client'
import type { AlertResponse, AlertStatus } from '../types'

// GET /api/alerts (optionally ?status=) -> AlertController.listAlerts
export function getAlerts(status?: AlertStatus): Promise<AlertResponse[]> {
  const query = status ? `?status=${status}` : ''
  return get<AlertResponse[]>(`/alerts${query}`)
}

// POST /api/alerts/{id}/acknowledge?acknowledgedBy= -> AlertController.acknowledge
export function acknowledgeAlert(id: string, acknowledgedBy: string): Promise<AlertResponse> {
  return post<AlertResponse>(`/alerts/${id}/acknowledge?acknowledgedBy=${encodeURIComponent(acknowledgedBy)}`)
}

// POST /api/alerts/{id}/resolve -> AlertController.resolve
export function resolveAlert(id: string): Promise<AlertResponse> {
  return post<AlertResponse>(`/alerts/${id}/resolve`)
}
