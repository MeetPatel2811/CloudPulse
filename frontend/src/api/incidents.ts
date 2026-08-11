import { get, post } from './client'
import type { IncidentResponse } from '../types'

export function getIncident(id: string): Promise<IncidentResponse> {
  return get<IncidentResponse>(`/incidents/${id}`)
}

export function assignIncidentOwner(id: string, owner: string): Promise<IncidentResponse> {
  return post<IncidentResponse>(`/incidents/${id}/assign`, { owner })
}

export function addIncidentNote(
  id: string,
  note: string,
  author = 'operator',
): Promise<IncidentResponse> {
  return post<IncidentResponse>(`/incidents/${id}/notes`, { note, author })
}
