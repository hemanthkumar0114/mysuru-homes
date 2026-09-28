import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { fetchMyListings } from '../api/client'
import { HomeIcon, PinIcon } from '../components/icons'
import { BlockSkeleton } from '../components/Skeleton'
import { formatRent, pluralize, typeLabel } from '../utils/format'

const STATUS = {
  DRAFT: { label: 'Pending verification', className: 'badge-pending' },
  LIVE: { label: 'Live', className: 'badge-verified' },
  EXPIRED: { label: 'Expired', className: 'badge-muted' },
}

export default function MyListings() {
  // null = still loading; otherwise { listings } or { error }.
  const [result, setResult] = useState(null)
  // Set by PostProperty when it sends the owner here after a submit.
  const { posted, photoWarning } = useLocation().state ?? {}

  useEffect(() => {
    fetchMyListings()
      .then((listings) => setResult({ listings }))
      .catch((err) => setResult({ error: err.message }))
  }, [])

  const listings = result?.listings ?? []

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <h1>My listings</h1>
          {listings.length > 0 && (
            <p className="text-muted">
              {listings.length} {listings.length === 1 ? 'property' : 'properties'}
            </p>
          )}
        </div>
        <Link to="/post-property" className="btn btn-primary">
          Post a property
        </Link>
      </div>

      {posted && (
        <p className="success-box notice" role="status">
          Submitted! &ldquo;{posted}&rdquo; is pending verification. Our field team will visit soon.
          {photoWarning}
        </p>
      )}

      {result === null && (
        <div className="stack" aria-hidden="true">
          <BlockSkeleton lines={1} />
          <BlockSkeleton lines={1} />
          <BlockSkeleton lines={1} />
        </div>
      )}

      {result?.error && <p className="empty-state text-danger">{result.error}</p>}

      {result?.listings && listings.length === 0 && (
        <div className="card no-results">
          <span className="empty-icon">
            <HomeIcon />
          </span>
          <h2>No properties yet</h2>
          <p className="text-muted">
            Post your first property and our field team will verify it before it goes live.
          </p>
          <Link to="/post-property" className="btn btn-primary">
            Post a property
          </Link>
        </div>
      )}

      {listings.length > 0 && (
        <ul className="my-listings">
          {listings.map((listing) => {
            const status = STATUS[listing.status] ?? {
              label: listing.status,
              className: 'badge-muted',
            }
            return (
              <li key={listing.id} className="card my-listing">
                <span className="my-listing-icon">
                  <HomeIcon />
                </span>

                <div className="my-listing-main">
                  <Link to={`/listings/${listing.id}`} className="my-listing-title">
                    {listing.title}
                  </Link>
                  <p className="listing-meta text-muted">
                    <PinIcon />
                    {listing.locality} · {typeLabel(listing.type)}
                  </p>
                  <p className="my-listing-activity">
                    <span>{pluralize(listing.enquiryCount, 'enquiry', 'enquiries')}</span>
                    <span>{pluralize(listing.visitCount, 'visit request')}</span>
                    <Link to={`/my-listings/${listing.id}`} className="activity-link">
                      View details
                    </Link>
                  </p>
                </div>

                <div className="my-listing-side">
                  <p className="price">
                    ₹{formatRent(listing.rentAmount)}
                    <span className="price-unit"> / month</span>
                  </p>
                  <span className={`badge ${status.className}`}>{status.label}</span>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
