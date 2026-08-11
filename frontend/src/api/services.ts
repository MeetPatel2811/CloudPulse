import { del, get, post, put } from './client'
import type { MonitoredService, ProbeServiceResponse, RegisterServiceRequest } from '../types'

// GET /api/services -> ServiceController.listServices
export function getServices(): Promise<MonitoredService[]> {
  return get<MonitoredService[]>('/services')
}

// GET /api/services/{id} -> ServiceController.getService
export function getService(id: string): Promise<MonitoredService> {
  return get<MonitoredService>(`/services/${id}`)
}

// POST /api/services -> ServiceController.registerService
export function registerService(request: RegisterServiceRequest): Promise<MonitoredService> {
  return post<MonitoredService>('/services', request)
}

// PUT /api/services/{id} -> ServiceController.updateService
export function updateService(id: string, request: RegisterServiceRequest): Promise<MonitoredService> {
  return put<MonitoredService>(`/services/${id}`, request)
}

// DELETE /api/services/{id} -> ServiceController.deleteService (204, or 409 if it has history)
export function deleteService(id: string): Promise<void> {
  return del<void>(`/services/${id}`)
}

// POST /api/services/{id}/check -> ServiceController.runCheck
export function runCheck(id: string): Promise<MonitoredService> {
  return post<MonitoredService>(`/services/${id}/check`)
}

// POST /api/services/{id}/enable -> ServiceController.enable
export function enableService(id: string): Promise<MonitoredService> {
  return post<MonitoredService>(`/services/${id}/enable`)
}

// POST /api/services/{id}/disable -> ServiceController.disable
export function disableService(id: string): Promise<MonitoredService> {
  return post<MonitoredService>(`/services/${id}/disable`)
}

// POST /api/services/probe checks a URL without registering or persisting it.
export function probeService(healthUrl: string): Promise<ProbeServiceResponse> {
  return post<ProbeServiceResponse>('/services/probe', { url: healthUrl })
}
