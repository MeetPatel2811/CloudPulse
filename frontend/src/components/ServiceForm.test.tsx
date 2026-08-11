import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ServiceForm } from './ServiceForm'

describe('ServiceForm', () => {
  it('submits the default HTTP shape without an expected keyword', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)

    render(
      <ServiceForm title="Add monitor" submitLabel="Save" onSubmit={onSubmit} onClose={vi.fn()} />,
    )

    await user.type(screen.getByLabelText('Name'), 'Payments API')
    await user.type(screen.getByLabelText('Health URL'), 'http://localhost:8081/health')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      name: 'Payments API',
      healthUrl: 'http://localhost:8081/health',
      monitorType: 'HTTP',
      checkIntervalSeconds: 15,
      serviceGroup: null,
      tags: [],
    }))
  })

  it('reveals and requires an expected keyword when KEYWORD is selected', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)

    render(
      <ServiceForm title="Add monitor" submitLabel="Save" onSubmit={onSubmit} onClose={vi.fn()} />,
    )

    expect(screen.queryByLabelText('Expected keyword')).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Monitor type'), 'KEYWORD')
    expect(screen.getByLabelText('Expected keyword')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Name'), 'Status Page')
    await user.type(screen.getByLabelText('Health URL'), 'https://status.example.com')
    await user.type(screen.getByLabelText('Expected keyword'), 'All Systems Operational')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      name: 'Status Page',
      healthUrl: 'https://status.example.com',
      monitorType: 'KEYWORD',
      checkIntervalSeconds: 15,
      expectedKeyword: 'All Systems Operational',
      serviceGroup: null,
      tags: [],
    }))
  })
})
