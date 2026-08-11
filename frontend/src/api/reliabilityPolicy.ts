import { get, patch } from './client'
import type { ReliabilityPolicyResponse } from '../types'

// GET /api/services/{id}/reliability-policy -> ReliabilityPolicyController.getPolicy
export function getReliabilityPolicy(serviceId: string): Promise<ReliabilityPolicyResponse> {
  return get<ReliabilityPolicyResponse>(`/services/${serviceId}/reliability-policy`)
}

// PATCH /api/services/{id}/reliability-policy -> ReliabilityPolicyController.updatePolicy
export function updateReliabilityPolicy(
  serviceId: string,
  latencyThresholdMs: number | null,
  alertFailureThreshold?: number,
): Promise<ReliabilityPolicyResponse> {
  return patch<ReliabilityPolicyResponse>(`/services/${serviceId}/reliability-policy`, {
    latencyThresholdMs,
    ...(alertFailureThreshold === undefined ? {} : { alertFailureThreshold }),
  })
}
