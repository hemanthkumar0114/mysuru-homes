import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchListingActivity } from '../api/client'
import { ArrowLeftIcon } from '../components/icons'
import VisitStatusBadge from '../components/VisitStatusBadge'
import { pluralize } from '../utils/format'
import { formatIstDate, formatIstDateTime } from '../utils/istTime'

// The owner's detail view for one property: who enquired, who asked to visit.
export default function ListingActivity() {
  const { id } = useParams()
  // null = loading; otherwise { activity } or { error }.
  const [result, setResult] = useState(null)

  useEffect(() => {
    fetchListingActivity(id)
      .then((activity) => setResult({ activity }))
      .catch((err) => setResult({ error: err.message }))
  }, [id])

  const activity = result?.activity

  return (
    <div className="container page">
      <Link to="/my-listings" className="back-link">
        <ArrowLeftIcon />
        Back to my listings
      </Link>

      {result === null && <p className="empty-state">Loading…</p>}

      {result?.error && (
        <div className="card no-results">
          <h2>Couldn&apos;t load this property</h2>
          <p className="text-muted">{result.error}</p>
        </div>
      )}

      {activity && (
        <>
          <div className="page-head">
            <div>
              <h1>{activity.title}</h1>
              <p className="text-muted">
                {pluralize(activity.enquiries.length, 'enquiry', 'enquiries')} ·{' '}
                {pluralize(activity.visits.length, 'visit request')}
              </p>
            </div>
            <Link to={`/listings/${activity.listingId}`} className="btn">
              View listing
            </Link>
          </div>

          <div className="activity-grid">
            <section className="card activity-card">
              <h2>Visit requests</h2>
              {activity.visits.length === 0 ? (
                <p className="text-muted">No one has asked to visit yet.</p>
              ) : (
                <ul className="activity-list">
                  {activity.visits.map((visit) => (
                    <li key={visit.id}>
                      <div>
                        <p className="activity-name">{visit.tenantName}</p>
                        <p className="text-muted text-sm">{formatIstDateTime(visit.slotTime)}</p>
                      </div>
                      <VisitStatusBadge status={visit.status} />
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="card activity-card">
              <h2>Enquiries</h2>
              {activity.enquiries.length === 0 ? (
                <p className="text-muted">
                  No one has tapped &ldquo;I&apos;m interested&rdquo; yet.
                </p>
              ) : (
                <ul className="activity-list">
                  {activity.enquiries.map((enquiry) => (
                    <li key={enquiry.id}>
                      <p className="activity-name">{enquiry.tenantName}</p>
                      <span className="text-muted text-sm">
                        {formatIstDate(enquiry.createdAt)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>

          <p className="text-muted text-sm activity-note">
            Tenants are shown by first name. Our team coordinates visit times and contacts you to
            confirm.
          </p>
        </>
      )}
    </div>
  )
}
