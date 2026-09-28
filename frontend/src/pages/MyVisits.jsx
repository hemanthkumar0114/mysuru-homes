import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelMyVisit, fetchMyVisits } from '../api/client'
import { CalendarIcon, PinIcon } from '../components/icons'
import { BlockSkeleton } from '../components/Skeleton'
import VisitStatusBadge from '../components/VisitStatusBadge'
import { isOpenVisit } from '../utils/format'
import { formatIstDateTime } from '../utils/istTime'

export default function MyVisits() {
  // null = still loading; otherwise { visits } or { error }.
  const [result, setResult] = useState(null)
  const [confirmingId, setConfirmingId] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [notice, setNotice] = useState(null) // { ok, message }

  useEffect(() => {
    fetchMyVisits()
      .then((visits) => setResult({ visits }))
      .catch((err) => setResult({ error: err.message }))
  }, [])

  async function handleCancel(visit) {
    setNotice(null)
    setBusyId(visit.id)
    try {
      const updated = await cancelMyVisit(visit.id)
      setResult((prev) => ({
        visits: prev.visits.map((v) => (v.id === visit.id ? updated : v)),
      }))
      setNotice({ ok: true, message: 'Your visit request was cancelled.' })
    } catch (err) {
      setNotice({ ok: false, message: err.message })
    } finally {
      setBusyId(null)
      setConfirmingId(null)
    }
  }

  const visits = result?.visits ?? []

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <h1>My visits</h1>
          <p className="text-muted">
            Visit requests you have made. All times are in India Standard Time (IST).
          </p>
        </div>
        <Link to="/" className="btn">
          Browse listings
        </Link>
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
          <BlockSkeleton lines={1} />
          <BlockSkeleton lines={1} />
        </div>
      )}

      {result?.error && <p className="empty-state text-danger">{result.error}</p>}

      {result?.visits && visits.length === 0 && (
        <div className="card no-results">
          <span className="empty-icon">
            <CalendarIcon />
          </span>
          <h2>No visit requests yet</h2>
          <p className="text-muted">
            Open any listing and use &ldquo;Book a visit&rdquo; to ask for a time to see it.
          </p>
          <Link to="/" className="btn btn-primary">
            Browse listings
          </Link>
        </div>
      )}

      {visits.length > 0 && (
        <ul className="my-listings">
          {visits.map((visit) => {
            const busy = busyId === visit.id
            return (
              <li key={visit.id} className="card visit-item">
                <div className="visit-item-main">
                  <Link to={`/listings/${visit.listingId}`} className="my-listing-title">
                    {visit.listingTitle}
                  </Link>
                  <p className="listing-meta text-muted">
                    <PinIcon />
                    {visit.locality}
                  </p>
                  <p className="visit-time">{formatIstDateTime(visit.slotTime)}</p>
                </div>

                <div className="visit-item-side">
                  <VisitStatusBadge status={visit.status} />
                  {isOpenVisit(visit.status) &&
                    (confirmingId === visit.id ? (
                      <div className="visit-actions">
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          disabled={busy}
                          onClick={() => handleCancel(visit)}
                        >
                          {busy ? 'Cancelling…' : 'Yes, cancel it'}
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm"
                          disabled={busy}
                          onClick={() => setConfirmingId(null)}
                        >
                          Keep it
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => {
                          setNotice(null)
                          setConfirmingId(visit.id)
                        }}
                      >
                        Cancel request
                      </button>
                    ))}
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
