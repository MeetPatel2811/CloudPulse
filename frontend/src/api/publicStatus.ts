import { get } from './client'
import type { PublicStatusResponse } from '../types'

export function getPublicStatus(): Promise<PublicStatusResponse> {
  return get<PublicStatusResponse>('/public/status')
}

