import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  deleteWebhook,
  getNotificationDeliveries,
  getWebhookTargets,
  registerWebhook,
} from '../api/webhooks'
import { NotificationCenter } from './NotificationCenter'

vi.mock('../api/webhooks', () => ({
  getWebhookTargets: vi.fn(),
  registerWebhook: vi.fn(),
  deleteWebhook: vi.fn(),
  getNotificationDeliveries: vi.fn(),
}))

const mockedGetWebhookTargets = vi.mocked(getWebhookTargets)
const mockedRegisterWebhook = vi.mocked(registerWebhook)
const mockedDeleteWebhook = vi.mocked(deleteWebhook)
const mockedGetNotificationDeliveries = vi.mocked(getNotificationDeliveries)

describe('NotificationCenter', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockedGetWebhookTargets.mockResolvedValue([{
      id: 'webhook-1',
      provider: 'SLACK',
      url: 'https://hooks.slack.com/services/secret-token',
      label: 'Ops Slack',
      enabled: true,
      createdAt: '2026-08-10T12:00:00Z',
    }])
    mockedGetNotificationDeliveries.mockResolvedValue([{
      id: 'delivery-1',
      provider: 'SLACK',
      targetUrl: 'https://hooks.slack.com/services/secret-token',
      serviceName: 'Payments API',
      message: 'Payments API is down',
      success: true,
      httpStatus: 200,
      error: null,
      sentAt: '2026-08-10T12:05:00Z',
    }])
  })

  it('loads protected delivery history, adds a destination, and removes one', async () => {
    const user = userEvent.setup()
    mockedRegisterWebhook.mockResolvedValue({
      id: 'webhook-2',
      provider: 'DISCORD',
      url: 'https://discord.com/api/webhooks/new-secret',
      label: 'Engineering Discord',
      enabled: true,
      createdAt: '2026-08-10T12:10:00Z',
    })
    mockedDeleteWebhook.mockResolvedValue()

    render(<NotificationCenter />)

    expect(await screen.findByText('Ops Slack')).toBeInTheDocument()
    expect(screen.getByText('Delivered')).toBeInTheDocument()
    expect(screen.getByText('Payments API')).toBeInTheDocument()
    expect(screen.queryByText(/secret-token/)).not.toBeInTheDocument()
    expect(screen.getAllByText('hooks.slack.com/••••••').length).toBeGreaterThan(0)

    await user.selectOptions(screen.getByLabelText('Provider'), 'DISCORD')
    await user.type(screen.getByLabelText(/Label/), 'Engineering Discord')
    await user.type(screen.getByLabelText('Webhook URL'), 'https://discord.com/api/webhooks/new-secret')
    await user.click(screen.getByRole('button', { name: 'Add destination' }))

    await waitFor(() => {
      expect(mockedRegisterWebhook).toHaveBeenCalledWith({
        provider: 'DISCORD',
        url: 'https://discord.com/api/webhooks/new-secret',
        label: 'Engineering Discord',
      })
    })
    expect(await screen.findByText('Engineering Discord')).toBeInTheDocument()

    const slackTarget = screen.getByText('Ops Slack').closest('article')
    if (!slackTarget) throw new Error('Slack destination was not rendered')
    await user.click(within(slackTarget).getByRole('button', { name: 'Remove' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Remove notification destination' })
    await user.click(within(dialog).getByRole('button', { name: 'Remove destination' }))

    await waitFor(() => expect(mockedDeleteWebhook).toHaveBeenCalledWith('webhook-1'))
    expect(screen.queryByText('Ops Slack')).not.toBeInTheDocument()
  })
})
