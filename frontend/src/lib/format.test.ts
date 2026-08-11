import { describe, expect, it } from 'vitest'
import { formatRefreshAge } from './format'

describe('formatRefreshAge', () => {
  it('shows exact elapsed seconds during the polling interval', () => {
    expect(formatRefreshAge(0)).toBe('0s ago')
    expect(formatRefreshAge(14.9)).toBe('14s ago')
  })

  it('uses compact minute and hour labels for delayed refreshes', () => {
    expect(formatRefreshAge(60)).toBe('1m ago')
    expect(formatRefreshAge(3_600)).toBe('1h ago')
  })

  it('never displays a negative age', () => {
    expect(formatRefreshAge(-2)).toBe('0s ago')
  })
})
