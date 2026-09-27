// Whole rupees show without decimals (₹7,500); rents with paise show two
// (₹12,500.50). Indian digit grouping: 1,25,000.
export function formatRent(amount) {
  const rent = Number(amount)
  return rent.toLocaleString('en-IN', {
    minimumFractionDigits: Number.isInteger(rent) ? 0 : 2,
    maximumFractionDigits: 2,
  })
}

export function bedroomsLabel(bedrooms) {
  if (bedrooms === 0) return 'Studio'
  return `${bedrooms} ${bedrooms === 1 ? 'bedroom' : 'bedrooms'}`
}

export function typeLabel(type) {
  return type === 'PG' ? 'PG / Co-living' : 'Rental'
}
