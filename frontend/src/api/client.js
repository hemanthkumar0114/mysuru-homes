// In dev, calls go through /api/* and /uploads/*, which Vite proxies to the
// Spring Boot server on :8080 (see vite.config.js). In production the
// frontend (Netlify) and backend (Render) are on different domains, so
// VITE_API_URL must be set at build time to the backend's full URL - see
// DEPLOY.md. Left empty, relative paths still work for same-origin setups.
const API_BASE_URL = import.meta.env.VITE_API_URL ?? ''

const TOKEN_KEY = 'mysuruhomes_token'

// Listing photo URLs come back from the API as either a path relative to the
// backend (local-disk storage) or an already-absolute URL (Cloudinary) - see
// PhotoStorageService. Relative ones need the backend origin prefixed so the
// browser doesn't request them from the frontend's own domain.
export function resolveMediaUrl(url) {
  if (!url) return url
  return /^https?:\/\//.test(url) ? url : `${API_BASE_URL}${url}`
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

// Turns a failed response into a sentence a person can act on. The backend
// sends {"message": "..."} for expected problems (bad input, wrong password).
// Server errors (5xx) are never shown raw - they may contain internal detail.
async function friendlyErrorMessage(res) {
  if (res.status >= 500) {
    return 'Something went wrong on our side. Please try again in a moment.'
  }
  try {
    const body = await res.json()
    if (body.message) return body.message
  } catch {
    // response had no JSON body - fall through to the generic messages
  }
  if (res.status === 401) return 'Please log in to continue.'
  if (res.status === 403) return "You don't have permission to do that."
  if (res.status === 404) return "We couldn't find what you were looking for."
  return 'That request could not be completed. Please check what you entered.'
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })

  if (!res.ok) {
    throw new Error(await friendlyErrorMessage(res))
  }

  // Some successful replies have no body (204, or a 201 from POST /api/visits).
  const body = await res.text()
  return body ? JSON.parse(body) : null
}

// ---- Public: browsing ----

export function fetchListings(params = {}) {
  const query = new URLSearchParams()
  if (params.locality) query.set('locality', params.locality)
  if (params.type) query.set('type', params.type)
  if (params.minRent) query.set('minRent', params.minRent)
  if (params.maxRent) query.set('maxRent', params.maxRent)
  if (params.bedrooms) query.set('bedrooms', params.bedrooms)
  if (params.lat != null) query.set('lat', params.lat)
  if (params.lng != null) query.set('lng', params.lng)
  if (params.radiusKm != null) query.set('radiusKm', params.radiusKm)
  const qs = query.toString()
  return request(`/api/listings${qs ? `?${qs}` : ''}`)
}

export async function fetchListingTotal() {
  const res = await fetch(`${API_BASE_URL}/api/listings?size=1`)
  if (!res.ok) {
    throw new Error(await friendlyErrorMessage(res))
  }
  const header = Number(res.headers.get('X-Total-Count'))
  if (Number.isFinite(header) && res.headers.has('X-Total-Count')) return header
  const body = await res.json()
  return body.length
}

export function fetchListing(id) {
  return request(`/api/listings/${id}`)
}

export function fetchLocalities() {
  return request('/api/localities')
}

export function fetchLocality(slug) {
  return request(`/api/localities/${encodeURIComponent(slug)}`)
}

// ---- Auth ----

export function registerUser(data) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function loginUser(data) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// ---- Owner ----

export function createListing(data) {
  return request('/api/listings', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function fetchMyListings() {
  return request('/api/my-listings')
}

// Photos go through fetch() directly (not the shared `request` helper), because a
// FormData body must NOT get a "Content-Type: application/json" header - the browser
// sets its own (with the multipart boundary) when it sees a FormData body.
export async function uploadListingPhotos(listingId, files) {
  const form = new FormData()
  for (const file of files) form.append('files', file)

  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API_BASE_URL}/api/listings/${listingId}/photos`, { method: 'POST', headers, body: form })
  if (!res.ok) {
    throw new Error(await friendlyErrorMessage(res))
  }
  return res.json()
}

// ---- Tenant actions ----

export function createEnquiry(listingId) {
  return request('/api/enquiries', {
    method: 'POST',
    body: JSON.stringify({ listingId }),
  })
}

export function bookVisit(listingId, slotTime) {
  return request('/api/visits', {
    method: 'POST',
    body: JSON.stringify({ listingId, slotTime }),
  })
}

// ---- Admin ----

export function fetchPendingListings() {
  return request('/api/admin/listings/pending')
}

export function verifyListing(id) {
  return request(`/api/admin/listings/${id}/verify`, { method: 'POST' })
}

export function fetchAdminVisits(status) {
  return request(`/api/admin/visits${status ? `?status=${status}` : ''}`)
}

export function confirmVisit(id) {
  return request(`/api/admin/visits/${id}/confirm`, { method: 'POST' })
}

export function cancelVisitAsAdmin(id) {
  return request(`/api/admin/visits/${id}/cancel`, { method: 'POST' })
}

// ---- Visits and owner activity ----

export function fetchMyVisits() {
  return request('/api/visits/mine')
}

export function cancelMyVisit(id) {
  return request(`/api/visits/${id}/cancel`, { method: 'POST' })
}

export function fetchListingActivity(listingId) {
  return request(`/api/my-listings/${listingId}/activity`)
}
