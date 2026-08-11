import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'
import { ServiceForm } from './ServiceForm'

function ServiceFormHarness() {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>Open service form</button>
      {open && (
        <ServiceForm
          title="Add Service"
          submitLabel="Add"
          onSubmit={vi.fn()}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  )
}

describe('accessible dialogs', () => {
  it('labels the service form, focuses its first field, closes with Escape, and restores focus', async () => {
    const user = userEvent.setup()
    render(<ServiceFormHarness />)
    const opener = screen.getByRole('button', { name: 'Open service form' })

    await user.click(opener)

    expect(screen.getByRole('dialog', { name: 'Add Service' })).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByLabelText('Name')).toHaveFocus()
    expect(screen.getByLabelText('Health URL')).toHaveAttribute('type', 'url')
    expect(document.body).toHaveStyle({ overflow: 'hidden' })

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
    expect(document.body).not.toHaveStyle({ overflow: 'hidden' })
  })

  it('labels a destructive confirmation and traps Tab focus inside it', async () => {
    const user = userEvent.setup()
    const onCancel = vi.fn()
    render(
      <ConfirmDialog
        title="Delete service"
        message="This cannot be undone."
        confirmLabel="Delete"
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />,
    )

    const dialog = screen.getByRole('alertdialog', { name: 'Delete service' })
    const cancelButton = screen.getByRole('button', { name: 'Cancel' })
    const deleteButton = screen.getByRole('button', { name: 'Delete' })
    expect(dialog).toHaveAccessibleDescription('This cannot be undone.')
    expect(cancelButton).toHaveFocus()

    await user.tab()
    expect(deleteButton).toHaveFocus()
    await user.tab()
    expect(cancelButton).toHaveFocus()

    await user.keyboard('{Escape}')
    expect(onCancel).toHaveBeenCalledOnce()
  })
})
