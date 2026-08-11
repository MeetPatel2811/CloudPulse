import { patch } from './client'
import type { EvaluationPolicy, EvaluationPolicyResponse } from '../types'

// PATCH /api/services/{id}/evaluation-policy -> EvaluationPolicyController.updatePolicy
export function updateEvaluationPolicy(
  serviceId: string,
  evaluationPolicy: EvaluationPolicy,
): Promise<EvaluationPolicyResponse> {
  return patch<EvaluationPolicyResponse>(`/services/${serviceId}/evaluation-policy`, {
    evaluationPolicy,
  })
}
