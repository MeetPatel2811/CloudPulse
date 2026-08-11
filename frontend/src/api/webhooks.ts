import { del, get, post } from './client'
import type {
  NotificationDeliveryResponse,
  RegisterWebhookRequest,
  WebhookTargetResponse,
} from '../types'

export function getWebhookTargets(): Promise<WebhookTargetResponse[]> {
  return get<WebhookTargetResponse[]>('/webhooks')
}

export function registerWebhook(request: RegisterWebhookRequest): Promise<WebhookTargetResponse> {
  return post<WebhookTargetResponse>('/webhooks', request)
}

export function deleteWebhook(id: string): Promise<void> {
  return del<void>(`/webhooks/${id}`)
}

export function getNotificationDeliveries(): Promise<NotificationDeliveryResponse[]> {
  return get<NotificationDeliveryResponse[]>('/notifications')
}
