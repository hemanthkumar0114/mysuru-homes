import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelVisitAsAdmin, confirmVisit, fetchAdminVisits } from '../api/client'
import VisitStatusBadge from '../components/VisitStatusBadge'
import { isOpenVisit } from '../utils/format'
import { formatIstDateTime } from '../utils/istTime'

const FILTERS = [
  { value: '', label: 'All' },
  { value: 'REQUESTED', label: 'Awaiting confirmation' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export default function AdminVisits() {
  const [filter, setFilter] = useState('')
  // null = loading; otherwise { visits } or { error }. `filter` records what it was loaded for.
  const [result, setResult] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    let ignore = false
    fetchAdminVisits(filter)
      .then((visits) => !ignore && setResult({ visits, filter }))
      .catch((err) => !ignore && setResult({ error: err.message, filter }))
    return () => {
      ignore = true
    }
  }, [filter])

  const loading = result === null || result.filter !== filter

  async function act(visit, action, doneMessage) {
    setNotice(null)
    setBusyId(visit.id)
    try {
      const updated = await action(visit.id)
      // Replace the row, or drop it if it no longer matches the filter that is open.
      setResult((prev) => ({
        ...prev,
        visits: prev.visits
          .map((v) => (v.id === visit.id ? updated : v))
          .filter((v) => !filter || v.status === filter),
      }))
      setNotice({ ok: true, message: doneMessage })
    } catch (err) {
      setNotice({ ok: false, message: err.message })
    } finally {
      setBusyId(null)
    }
  }

  const visits = result?.visits ?? []

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <h1>Visit requests</h1>
          <p className="text-muted">
            Contact the tenant, agree a time with the owner, then confirm. Times are in India
            Standard Time (IST).
          </p>
        </div>
      </div>

      <div className="tab-row" role="group" aria-label="Filter by status">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            className={filter === f.value ? 'tab active' : 'tab'}
            aria-pressed={filter === f.value}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {notice && (
        <p
          className={`${notice.ok ? 'success-box' : 'error-box'} notice`}
          role={notice.ok ? 'status' : 'alert'}
        >
          {notice.message}
        </p>
      )}

      {loading && <p className="empty-state">Loading…</p>}

      {!loading && result.error && <p className="empty-state text-danger">{result.error}</p>}

      {!loading && result.visits && visits.length === 0 && (
        <div className="card no-results">
          <h2>No visit requests here</h2>
          <p className="text-muted">Nothing matches this filter right now.</p>
        </div>
      )}

      {!loading && visits.length > 0 && (
        <ul className="review-list">
          {visits.map((visit) => {
            const busy = busyId === visit.id
            return (
              <li key={visit.id} className="card review-card">
                <div className="review-head">
                  <div className="review-heading">
                    <Link
                      to={`/listings/${visit.listingId}`}
                      className="review-title"
                      target="_blank"
                      rel="noreferrer"
                    >
                      {visit.listingTitle}
                    </Link>
                    <VisitStatusBadge status={visit.status} />
                  </div>
                  <p className="visit-time">{formatIstDateTime(visit.slotTime)}</p>
                </div>

                <dl className="review-facts">
                  <div>
                    <dt>Tenant</dt>
                    <dd>{visit.tenantName}</dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{visit.tenantEmail}</dd>
                  </div>
                  <div>
                    <dt>Locality</dt>
                    <dd>{visit.locality}</dd>
                  </div>
                  <div>
                    <dt>Requested</dt>
                    <dd>{formatIstDateTime(visit.createdAt)}</dd>
                  </div>
                </dl>

                {isOpenVisit(visit.status) && (
                  <div className="review-actions">
                    {visit.status === 'REQUESTED' && (
                      <button
                        type="button"
                        className="btn btn-primary"
                        disabled={busy}
                        onClick={() => act(visit, confirmVisit, 'Visit confirmed.')}
                      >
                        {busy ? 'Working…' : 'Confirm visit'}
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn"
                      disabled={busy}
                      onClick={() => act(visit, cancelVisitAsAdmin, 'Visit cancelled.')}
                    >
                      Cancel visit
                    </button>
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
