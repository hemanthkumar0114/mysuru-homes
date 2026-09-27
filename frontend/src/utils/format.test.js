import { describe, expect, it } from 'vitest'
import {
  bedroomsLabel,
  formatRent,
  isOpenVisit,
  pluralize,
  rentRangeLabel,
  typeLabel,
  visitStatus,
} from './format'

describe('formatRent', () => {
  it('formats a whole rupee amount with no decimals', () => {
    expect(formatRent(7500)).toBe('7,500')
  })

  it('uses Indian digit grouping (lakhs, not thousands)', () => {
    expect(formatRent(125000)).toBe('1,25,000')
  })

  it('keeps two decimal places when the amount has paise', () => {
    expect(formatRent(12500.5)).toBe('12,500.50')
  })

  it('accepts a numeric string, as rentAmount arrives from JSON as one', () => {
    expect(formatRent('9000')).toBe('9,000')
  })

  it('formats zero without decimals', () => {
    expect(formatRent(0)).toBe('0')
  })
})

describe('rentRangeLabel', () => {
  it('returns null when either end is missing (no live listings yet)', () => {
    expect(rentRangeLabel(null, 12000)).toBeNull()
    expect(rentRangeLabel(9000, null)).toBeNull()
    expect(rentRangeLabel(null, null)).toBeNull()
  })

  it('shows a single amount when min and max are equal', () => {
    expect(rentRangeLabel(9000, 9000)).toBe('₹9,000 / month')
  })

  it('shows a range when min and max differ', () => {
    expect(rentRangeLabel(9000, 14000)).toBe('₹9,000 – ₹14,000 / month')
  })
})

describe('bedroomsLabel', () => {
  it('calls zero bedrooms a studio', () => {
    expect(bedroomsLabel(0)).toBe('Studio')
  })

  it('uses the singular for exactly one bedroom', () => {
    expect(bedroomsLabel(1)).toBe('1 bedroom')
  })

  it('uses the plural for more than one bedroom', () => {
    expect(bedroomsLabel(3)).toBe('3 bedrooms')
  })
})

describe('typeLabel', () => {
  it('labels PG listings', () => {
    expect(typeLabel('PG')).toBe('PG / Co-living')
  })

  it('labels everything else as a rental', () => {
    expect(typeLabel('RENT')).toBe('Rental')
  })
})

describe('visitStatus', () => {
  it('gives each known status a human label and a badge tone', () => {
    expect(visitStatus('REQUESTED')).toEqual({ label: 'Awaiting confirmation', tone: 'badge-pending' })
    expect(visitStatus('CONFIRMED')).toEqual({ label: 'Confirmed', tone: 'badge-verified' })
    expect(visitStatus('CANCELLED')).toEqual({ label: 'Cancelled', tone: 'badge-muted' })
  })

  it('falls back to the raw value for an unrecognised status', () => {
    expect(visitStatus('SOMETHING_NEW')).toEqual({ label: 'SOMETHING_NEW', tone: 'badge-muted' })
  })
})

describe('isOpenVisit', () => {
  it('treats REQUESTED and CONFIRMED as open', () => {
    expect(isOpenVisit('REQUESTED')).toBe(true)
    expect(isOpenVisit('CONFIRMED')).toBe(true)
  })

  it('treats COMPLETED and CANCELLED as not open', () => {
    expect(isOpenVisit('COMPLETED')).toBe(false)
    expect(isOpenVisit('CANCELLED')).toBe(false)
  })
})

describe('pluralize', () => {
  it('uses the singular for a count of one', () => {
    expect(pluralize(1, 'enquiry', 'enquiries')).toBe('1 enquiry')
  })

  it('uses the given plural for other counts', () => {
    expect(pluralize(0, 'enquiry', 'enquiries')).toBe('0 enquiries')
    expect(pluralize(3, 'enquiry', 'enquiries')).toBe('3 enquiries')
  })

  it('defaults the plural to singular + "s" when none is given', () => {
    expect(pluralize(2, 'visit request')).toBe('2 visit requests')
    expect(pluralize(1, 'visit request')).toBe('1 visit request')
  })
})
