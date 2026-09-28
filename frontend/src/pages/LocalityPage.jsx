import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchLocality } from '../api/client'
import { ArchPattern } from '../components/Patterns'
import { ArrowLeftIcon, PinIcon, SearchIcon } from '../components/icons'
import ListingCard from '../components/ListingCard'
import { ListingGridSkeleton } from '../components/Skeleton'
import { formatRent, rentRangeLabel } from '../utils/format'

export default function LocalityPage() {
  const { slug } = useParams()
  // null = loading; otherwise { locality } or { error }. `slug` records what it was loaded for.
  const [result, setResult] = useState(null)

  useEffect(() => {
    let ignore = false
    fetchLocality(slug)
      .then((locality) => !ignore && setResult({ locality, slug }))
      .catch((err) => !ignore && setResult({ error: err.message, slug }))
    return () => {
      ignore = true
    }
  }, [slug])

  const loading = result === null || result.slug !== slug
  const locality = loading ? null : result.locality

  return (
    <div className="container page">
      <Link to="/" className="back-link">
        <ArrowLeftIcon />
        Back to all listings
      </Link>

      {loading && (
        <>
          <div className="skeleton" style={{ height: 260, borderRadius: 'var(--radius-lg)', marginBottom: 'var(--space-6)' }} />
          <ListingGridSkeleton count={3} />
        </>
      )}

      {!loading && result.error && (
        <div className="card no-results">
          <span className="empty-icon">
            <PinIcon />
          </span>
          <h2>Locality not found</h2>
          <p className="text-muted">{result.error}</p>
          <Link to="/" className="btn btn-primary">
            Browse all listings
          </Link>
        </div>
      )}

      {locality && (
        <>
          <div className="locality-banner">
            <div className="locality-banner-pattern">
              <ArchPattern id="locality-arches" />
            </div>
            <header className="locality-head">
              <p className="locality-eyebrow">
                <PinIcon />
                Mysuru
              </p>
              <h1>Renting in {locality.name}</h1>
              <p className="locality-about">{locality.description}</p>
            </header>

            <dl className="locality-stats">
              <div>
                <dt>Live listings</dt>
                <dd>{locality.listingCount}</dd>
              </div>
              <div>
                <dt>Rent range now</dt>
                <dd>{rentRangeLabel(locality.minRent, locality.maxRent) ?? 'No live listings yet'}</dd>
              </div>
              {locality.typicalRent != null && (
                <div>
                  <dt>Typical rent</dt>
                  <dd>about ₹{formatRent(locality.typicalRent)} / month</dd>
                </div>
              )}
            </dl>
          </div>

          <h2 className="locality-section-title">Available in {locality.name}</h2>

          {locality.listings.length === 0 ? (
            <div className="card no-results">
              <span className="empty-icon">
                <SearchIcon />
              </span>
              <h2>No live listings in {locality.name} right now</h2>
              <p className="text-muted">
                New verified homes are added regularly. Meanwhile, see what is available nearby.
              </p>
              <Link to="/" className="btn btn-primary">
                Browse all listings
              </Link>
            </div>
          ) : (
            <div className="grid">
              {locality.listings.map((listing) => (
                <ListingCard key={listing.id} listing={listing} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
