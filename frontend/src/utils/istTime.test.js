import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatIstDate, formatIstDateTime, isFutureIst, istInputToInstant, nowIstForInput } from './istTime'

describe('istInputToInstant', () => {
  it('treats the datetime-local value as India Standard Time (+05:30)', () => {
    // 9:42am IST on 27 Sept 2026 is 4:12am UTC the same day.
    expect(istInputToInstant('2026-09-27T09:42')).toBe('2026-09-27T04:12:00.000Z')
  })

  it('handles midnight correctly (rolls into the previous UTC day)', () => {
    // Midnight IST is 6:30pm UTC the previous day.
    expect(istInputToInstant('2026-01-01T00:00')).toBe('2025-12-31T18:30:00.000Z')
  })

  it('accepts a value that already has seconds', () => {
    expect(istInputToInstant('2026-09-27T09:42:30')).toBe('2026-09-27T04:12:30.000Z')
  })
})

describe('isFutureIst', () => {
  beforeEach(() => {
    // A fixed "now" so the future/past comparison is deterministic.
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-27T04:12:00.000Z')) // 9:42am IST
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('is true for an IST time later than now', () => {
    expect(isFutureIst('2026-09-27T10:00')).toBe(true)
  })

  it('is false for an IST time earlier than now', () => {
    expect(isFutureIst('2026-09-27T09:00')).toBe(false)
  })

  it('is false for an empty or unparsable value', () => {
    expect(isFutureIst('')).toBe(false)
    expect(isFutureIst('not-a-date')).toBe(false)
  })
})

describe('nowIstForInput', () => {
  it('renders the current time as IST wall-clock text for a datetime-local input', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-27T04:12:00.000Z')) // 9:42am IST

    expect(nowIstForInput()).toBe('2026-09-27T09:42')

    vi.useRealTimers()
  })
})

describe('formatIstDateTime', () => {
  it('renders an instant as a human IST date and time, marked "IST"', () => {
    expect(formatIstDateTime('2026-09-27T04:12:00Z')).toBe('27 Sept 2026, 9:42 am IST')
  })

  it('is unaffected by the instant already carrying a different offset', () => {
    // Same instant as above, just written with an explicit +05:30 offset.
    expect(formatIstDateTime('2026-09-27T09:42:00+05:30')).toBe('27 Sept 2026, 9:42 am IST')
  })
})

describe('formatIstDate', () => {
  it('renders an instant as a human IST date with no time', () => {
    expect(formatIstDate('2026-09-11T06:26:05Z')).toBe('11 Sept 2026')
  })

  it('shows the IST calendar day even when UTC is still on the previous day', () => {
    // 11:30pm UTC on the 26th is 5:00am IST on the 27th.
    expect(formatIstDate('2026-09-26T23:30:00Z')).toBe('27 Sept 2026')
  })
})
