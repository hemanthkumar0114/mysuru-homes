// Whole rupees show without decimals (₹7,500); rents with paise show two
// (₹12,500.50). Indian digit grouping: 1,25,000.
export function formatRent(amount) {
  const rent = Number(amount)
  return rent.toLocaleString('en-IN', {
    minimumFractionDigits: Number.isInteger(rent) ? 0 : 2,
    maximumFractionDigits: 2,
  })
}

// "₹9,000 – ₹14,000 / month", "₹9,000 / month" when min = max, null when there is no rent yet.
export function rentRangeLabel(min, max) {
  if (min == null || max == null) return null
  if (Number(min) === Number(max)) return `₹${formatRent(min)} / month`
  return `₹${formatRent(min)} – ₹${formatRent(max)} / month`
}

export function bedroomsLabel(bedrooms) {
  if (bedrooms === 0) return 'Studio'
  return `${bedrooms} ${bedrooms === 1 ? 'bedroom' : 'bedrooms'}`
}

export function typeLabel(type) {
  return type === 'PG' ? 'PG / Co-living' : 'Rental'
}

// Visit request statuses, worded for people. `tone` picks the badge colour.
const VISIT_STATUS = {
  REQUESTED: { label: 'Awaiting confirmation', tone: 'badge-pending' },
  CONFIRMED: { label: 'Confirmed', tone: 'badge-verified' },
  COMPLETED: { label: 'Completed', tone: 'badge-type' },
  CANCELLED: { label: 'Cancelled', tone: 'badge-muted' },
}

export function visitStatus(status) {
  return VISIT_STATUS[status] ?? { label: status, tone: 'badge-muted' }
}

// A request that can still be confirmed or cancelled.
export function isOpenVisit(status) {
  return status === 'REQUESTED' || status === 'CONFIRMED'
}

// pluralize(1, 'enquiry', 'enquiries') -> "1 enquiry"; pluralize(3, 'visit request') -> "3 visit requests"
export function pluralize(count, singular, plural = `${singular}s`) {
  return `${count} ${count === 1 ? singular : plural}`
}
