export interface ListingSummary {
  id: string
  title: string
  locality: string
  type: 'RENT' | 'PG'
  rentAmount: number
  bedrooms: number | null
  lat: number
  lng: number
  verified: boolean
}

// In dev this goes through the Vite proxy configured in vite.config.ts,
// which forwards /api/* to the Spring Boot server on :8080.
export async function fetchListings(params?: {
  locality?: string
}): Promise<ListingSummary[]> {
  const query = params?.locality
    ? `?locality=${encodeURIComponent(params.locality)}`
    : ''
  const res = await fetch(`/api/listings${query}`)
  if (!res.ok) {
    throw new Error(`Failed to load listings: ${res.status}`)
  }
  return res.json()
}
