import { get } from './client'
import type { SslCertificateResponse } from '../types'

// GET /api/services/{id}/ssl-certificate -> SslCertificateController.check
export function getSslCertificate(serviceId: string): Promise<SslCertificateResponse> {
  return get<SslCertificateResponse>(`/services/${serviceId}/ssl-certificate`)
}
