// All calls go through /api/*, which Vite proxies to the Spring Boot
// server on :8080 during development (see vite.config.js).

const TOKEN_KEY = 'mysuruhomes_token'

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

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(path, { ...options, headers })

  if (!res.ok) {
    let message = `Request failed: ${res.status}`
    try {
      const body = await res.json()
      message = body.message || message
    } catch {
      // response had no JSON body, keep the default message
    }
    throw new Error(message)
  }

  if (res.status === 204) {
    return null
  }
  return res.json()
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

export function fetchListing(id) {
  return request(`/api/listings/${id}`)
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
