import { del, get, post } from './client'
import type { MaintenanceWindowResponse } from '../types'

// GET /api/services/{id}/maintenance-windows -> MaintenanceWindowController.listForService
export function getMaintenanceWindows(serviceId: string): Promise<MaintenanceWindowResponse[]> {
  return get<MaintenanceWindowResponse[]>(`/services/${serviceId}/maintenance-windows`)
}

// POST /api/services/{id}/maintenance-windows -> MaintenanceWindowController.schedule
export function scheduleMaintenanceWindow(
  serviceId: string,
  request: { startsAt: string; endsAt: string; reason?: string },
): Promise<MaintenanceWindowResponse> {
  return post<MaintenanceWindowResponse>(`/services/${serviceId}/maintenance-windows`, request)
}

// DELETE /api/services/{id}/maintenance-windows/{windowId} -> MaintenanceWindowController.cancel
export function cancelMaintenanceWindow(serviceId: string, windowId: string): Promise<void> {
  return del<void>(`/services/${serviceId}/maintenance-windows/${windowId}`)
}
