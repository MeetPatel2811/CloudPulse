import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge, statusStyle } from './StatusBadge'

describe('StatusBadge', () => {
  it('renders the user-facing label for a known backend status', () => {
    render(<StatusBadge status="RECOVERED" />)

    expect(screen.getByText('Recovered')).toBeInTheDocument()
  })

  it('falls back to Unknown for an unrecognized status', () => {
    render(<StatusBadge status="unexpected" />)

    expect(screen.getByText('Unknown')).toBeInTheDocument()
    expect(statusStyle('unexpected')).toEqual(statusStyle('UNKNOWN'))
  })
})
