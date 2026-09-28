import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchPendingListings, verifyListing } from '../api/client'
import { CheckIcon } from '../components/icons'
import { BlockSkeleton } from '../components/Skeleton'
import { bedroomsLabel, formatRent, typeLabel } from '../utils/format'
import { formatIstDate } from '../utils/istTime'

export default function AdminModeration() {
  // null = still loading; otherwise { listings } or { error }.
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [confirmingId, setConfirmingId] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [notice, setNotice] = useState(null) // { ok: boolean, message: string }

  useEffect(() => {
    let ignore = false
    fetchPendingListings()
      .then((listings) => !ignore && setResult({ listings }))
      .catch((err) => !ignore && setResult({ error: err.message }))
    // If the effect re-runs (retry), drop the old answer so it can't overwrite the new one.
    return () => {
      ignore = true
    }
  }, [attempt])

  function retry() {
    setResult(null)
    setAttempt((n) => n + 1)
  }

  async function handleVerify(listing) {
    setNotice(null)
    setBusyId(listing.id)
    try {
      await verifyListing(listing.id)
      setResult((prev) => ({ listings: prev.listings.filter((l) => l.id !== listing.id) }))
      setNotice({ ok: true, message: `Verified: “${listing.title}” is now live.` })
    } catch (err) {
      setNotice({ ok: false, message: err.message })
    } finally {
      setBusyId(null)
      setConfirmingId(null)
    }
  }

  const listings = result?.listings ?? []

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <h1>
            Pending verification
            {listings.length > 0 && <span className="count-pill">{listings.length}</span>}
          </h1>
          <p className="text-muted">
            Confirm the field team has physically visited and photographed each listing before
            making it live.
          </p>
        </div>
      </div>

      {notice && (
        <p
          className={`${notice.ok ? 'success-box' : 'error-box'} notice`}
          role={notice.ok ? 'status' : 'alert'}
        >
          {notice.message}
        </p>
      )}

      {result === null && (
        <div className="stack" aria-hidden="true">
          <BlockSkeleton />
          <BlockSkeleton />
        </div>
      )}

      {result?.error && (
        <div className="card no-results">
          <h2>Could not load the queue</h2>
          <p className="text-muted">{result.error}</p>
          <button type="button" className="btn btn-primary" onClick={retry}>
            Try again
          </button>
        </div>
      )}

      {result?.listings && listings.length === 0 && (
        <div className="card no-results">
          <span className="caught-up-icon">
            <CheckIcon />
          </span>
          <h2>All caught up</h2>
          <p className="text-muted">No listings are waiting for verification.</p>
        </div>
      )}

      {listings.length > 0 && (
        <ul className="review-list">
          {listings.map((listing) => {
            const busy = busyId === listing.id
            return (
              <li key={listing.id} className="card review-card">
                <div className="review-head">
                  <div className="review-heading">
                    <Link
                      to={`/listings/${listing.id}`}
                      className="review-title"
                      target="_blank"
                      rel="noreferrer"
                    >
                      {listing.title}
                    </Link>
                    <span className="badge badge-type">{typeLabel(listing.type)}</span>
                  </div>
                  <p className="price">
                    ₹{formatRent(listing.rentAmount)}
                    <span className="price-unit"> / month</span>
                  </p>
                </div>

                <dl className="review-facts">
                  <div>
                    <dt>Address</dt>
                    <dd>{listing.addressLine}</dd>
                  </div>
                  <div>
                    <dt>Locality</dt>
                    <dd>{listing.locality}</dd>
                  </div>
                  <div>
                    <dt>Owner</dt>
                    <dd>{listing.ownerName}</dd>
                  </div>
                  <div>
                    <dt>Bedrooms</dt>
                    <dd>{listing.bedrooms != null ? bedroomsLabel(listing.bedrooms) : '—'}</dd>
                  </div>
                  <div>
                    <dt>Submitted</dt>
                    <dd>{formatIstDate(listing.createdAt)}</dd>
                  </div>
                </dl>

                <div className="review-actions">
                  {confirmingId === listing.id ? (
                    <>
                      <p className="review-confirm">
                        Has our field team visited and photographed this property?
                      </p>
                      <button
                        type="button"
                        className="btn btn-primary"
                        disabled={busy}
                        onClick={() => handleVerify(listing)}
                      >
                        {busy ? 'Verifying…' : 'Yes, make it live'}
                      </button>
                      <button
                        type="button"
                        className="btn"
                        disabled={busy}
                        onClick={() => setConfirmingId(null)}
                      >
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={() => {
                        setNotice(null)
                        setConfirmingId(listing.id)
                      }}
                    >
                      Mark verified
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
