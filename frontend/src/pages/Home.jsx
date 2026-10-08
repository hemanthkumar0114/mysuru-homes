import { useEffect, useState } from 'react'
import { fetchListings, fetchListingTotal, fetchLocalities } from '../api/client'
import { ArchPattern } from '../components/Patterns'
import ListingCard from '../components/ListingCard'
import LocalityLinks from '../components/LocalityLinks'
import { ListingGridSkeleton } from '../components/Skeleton'
import { CameraIcon, KeyIcon, NoBrokerIcon, SearchIcon, ShieldIcon } from '../components/icons'

const HOW_IT_WORKS = [
  {
    icon: <SearchIcon />,
    title: 'Browse verified homes',
    description:
      'Filter by locality, rent and bedrooms to find rentals and PGs that are already field-checked.',
  },
  {
    icon: <KeyIcon />,
    title: 'Book a visit',
    description:
      'Tell us you’re interested or request a time to see the property in person, no broker in between.',
  },
  {
    icon: <NoBrokerIcon />,
    title: 'Move in, owner-direct',
    description:
      'Deal directly with the owner from enquiry to move-in. We just handle the introductions and verification.',
  },
]

const TRUST_POINTS = [
  {
    icon: <ShieldIcon />,
    title: 'Physically verified',
    description: 'Our field team visits every property before it goes live, so listings match reality.',
  },
  {
    icon: <CameraIcon />,
    title: 'Real photos',
    description: 'Photos are taken on-site by our team or the owner, never stock images.',
  },
  {
    icon: <NoBrokerIcon />,
    title: 'No broker fees',
    description: 'Owners list directly. You talk to the person who actually owns the property.',
  },
]

const EMPTY_FILTERS = {
  locality: '',
  type: '',
  minRent: '',
  maxRent: '',
  bedrooms: '',
}

export default function Home() {
  // useState = memory that survives re-renders. Changing it re-draws the page.
  const [filters, setFilters] = useState(EMPTY_FILTERS) // what is in the boxes right now
  const [applied, setApplied] = useState(EMPTY_FILTERS) // what we last searched for
  const [result, setResult] = useState({ key: null, listings: [], error: '' })
  const [stats, setStats] = useState(null) // { listingCount, localityCount } or null

  // Fetched once, independent of the filters above - the hero shows real,
  // unfiltered totals ("N verified homes"), not the current search result count.
  useEffect(() => {
    let ignore = false
    Promise.all([fetchListingTotal(), fetchLocalities()])
      .then(([listingCount, localities]) => {
        if (ignore) return
        setStats({ listingCount, localityCount: localities.length })
      })
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  const hasActiveFilters = Object.values(filters).some(Boolean)
  const rangeInvalid =
    filters.minRent !== '' &&
    filters.maxRent !== '' &&
    Number(filters.minRent) > Number(filters.maxRent)

  // Effect 1: wait until the user pauses typing (400ms) before searching, so
  // typing "15000" doesn't fire five requests. The returned function is
  // "cleanup": React runs it before the next run, cancelling the old timer.
  useEffect(() => {
    if (rangeInvalid) return undefined
    const timer = setTimeout(() => setApplied(filters), 400)
    return () => clearTimeout(timer)
  }, [filters, rangeInvalid])

  // Effect 2: whenever the applied filters change, ask the backend.
  useEffect(() => {
    let ignore = false
    const key = JSON.stringify(applied)

    fetchListings(applied)
      .then((listings) => {
        if (!ignore) setResult({ key, listings, error: '' })
      })
      .catch((err) => {
        if (ignore) return
        const error =
          err.name === 'TypeError'
            ? 'Could not reach the API. Is the backend running?'
            : err.message
        setResult({ key, listings: [], error })
      })

    // If filters change again before this answer arrives, throw the old
    // answer away so a slow, stale response can't overwrite a newer one.
    return () => {
      ignore = true
    }
  }, [applied])

  // Loading = "the results on screen are for a different search than `applied`".
  const loading = result.key !== JSON.stringify(applied)

  function updateFilter(name) {
    return (event) => setFilters((current) => ({ ...current, [name]: event.target.value }))
  }

  function clearFilters() {
    setFilters(EMPTY_FILTERS)
    setApplied(EMPTY_FILTERS)
  }

  const count = result.listings.length

  return (
    <>
      <section className="hero">
        <div className="hero-pattern">
          <ArchPattern id="hero-arches" />
        </div>
        <div className="container center-text hero-inner">
          <span className="hero-eyebrow">
            <ShieldIcon />
            Field-verified listings across Mysuru
          </span>
          <h1>Verified rentals in Mysuru, owner-direct.</h1>
          <p className="subtitle" style={{ margin: '0 auto' }}>
            Every listing is physically visited and photographed by our team.
            No broker spam.
          </p>

          {stats && (
            <div className="hero-stats">
              <div className="hero-stat">
                <strong>{stats.listingCount}</strong>
                <span>{stats.listingCount === 1 ? 'live listing' : 'live listings'}</span>
              </div>
              <div className="hero-stat">
                <strong>{stats.localityCount}</strong>
                <span>{stats.localityCount === 1 ? 'locality covered' : 'localities covered'}</span>
              </div>
              <div className="hero-stat">
                <strong>100%</strong>
                <span>owner-direct</span>
              </div>
            </div>
          )}
        </div>
      </section>

      <div className="container">
        <form
          className="card filter-bar"
          aria-label="Search filters"
          onSubmit={(e) => e.preventDefault()}
        >
          <div className="filter-grid">
            <div className="field filter-locality">
              <label htmlFor="locality">Locality</label>
              <input
                id="locality"
                placeholder="e.g. Vijayanagar, Hebbal"
                value={filters.locality}
                onChange={updateFilter('locality')}
              />
            </div>

            <div className="field">
              <label htmlFor="type">Type</label>
              <select id="type" value={filters.type} onChange={updateFilter('type')}>
                <option value="">Any</option>
                <option value="RENT">Rental</option>
                <option value="PG">PG / Co-living</option>
              </select>
            </div>

            <div className="field">
              <label htmlFor="minRent">Min rent (₹)</label>
              <input
                id="minRent"
                type="number"
                min="0"
                step="500"
                placeholder="No min"
                value={filters.minRent}
                onChange={updateFilter('minRent')}
              />
            </div>

            <div className="field">
              <label htmlFor="maxRent">Max rent (₹)</label>
              <input
                id="maxRent"
                type="number"
                min="0"
                step="500"
                placeholder="No max"
                value={filters.maxRent}
                onChange={updateFilter('maxRent')}
              />
            </div>

            <div className="field">
              <label htmlFor="bedrooms">Bedrooms</label>
              <select id="bedrooms" value={filters.bedrooms} onChange={updateFilter('bedrooms')}>
                <option value="">Any</option>
                <option value="1">1+</option>
                <option value="2">2+</option>
                <option value="3">3+</option>
                <option value="4">4+</option>
              </select>
            </div>

            <button
              type="button"
              className="btn filter-clear"
              onClick={clearFilters}
              disabled={!hasActiveFilters}
            >
              Clear filters
            </button>
          </div>

          {rangeInvalid && (
            <p className="filter-hint text-danger">
              Minimum rent is higher than maximum rent. Adjust one of them to search.
            </p>
          )}
        </form>
      </div>

      <div className="container page">
        {result.error ? (
          <p className="empty-state text-danger">{result.error}</p>
        ) : result.key === null ? (
          <ListingGridSkeleton />
        ) : (
          <>
            <p className="results-count" aria-live="polite">
              {loading
                ? 'Searching…'
                : `${count} ${count === 1 ? 'property' : 'properties'} found`}
            </p>

            {count === 0 && !loading ? (
              <div className="card no-results">
                <span className="empty-icon">
                  <SearchIcon />
                </span>
                <h2>
                  {hasActiveFilters ? 'No listings match your filters' : 'No live listings yet'}
                </h2>
                <p className="text-muted">
                  {hasActiveFilters
                    ? 'Try a wider rent range, fewer bedrooms, or clear a filter.'
                    : 'Check back soon - new verified homes are added regularly.'}
                </p>
                {hasActiveFilters && (
                  <button type="button" className="btn btn-primary" onClick={clearFilters}>
                    Clear all filters
                  </button>
                )}
              </div>
            ) : (
              <div className={loading ? 'grid is-loading' : 'grid'}>
                {result.listings.map((listing) => (
                  <ListingCard key={listing.id} listing={listing} />
                ))}
              </div>
            )}
          </>
        )}
      </div>

      <section className="how-it-works">
        <div className="container">
          <div className="section-head">
            <h2>How it works</h2>
            <p className="text-muted">
              From first search to move-in, three steps, no broker in the middle.
            </p>
          </div>
          <div className="how-grid">
            {HOW_IT_WORKS.map((step, index) => (
              <div key={step.title} className="how-step">
                <span className="how-step-num" aria-hidden="true">
                  {String(index + 1).padStart(2, '0')}
                </span>
                <span className="how-step-icon">{step.icon}</span>
                <h3>{step.title}</h3>
                <p className="text-muted">{step.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="trust-section">
        <div className="container">
          <div className="section-head">
            <h2>Every listing, physically verified</h2>
            <p className="text-muted">
              We don&apos;t just take an owner&apos;s word for it. Our field team visits, checks
              and photographs each property before it goes live.
            </p>
          </div>
          <div className="trust-grid">
            {TRUST_POINTS.map((point) => (
              <div key={point.title} className="trust-point">
                <span className="trust-point-icon">{point.icon}</span>
                <h3>{point.title}</h3>
                <p className="text-muted">{point.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="container page">
        <LocalityLinks />
      </div>
    </>
  )
}
