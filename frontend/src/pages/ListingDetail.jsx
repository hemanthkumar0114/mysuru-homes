import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { bookVisit, createEnquiry, fetchListing } from '../api/client'
import {
  ArrowLeftIcon,
  BedIcon,
  CheckIcon,
  HomeIcon,
  PinIcon,
  ShieldIcon,
} from '../components/icons'
import { useAuth } from '../context/AuthContext'
import { bedroomsLabel, formatRent, typeLabel } from '../utils/format'
import { isFutureIst, istInputToInstant, nowIstForInput } from '../utils/istTime'

export default function ListingDetail() {
  const { id } = useParams()
  const { isLoggedIn } = useAuth()

  const [listing, setListing] = useState(null)
  const [status, setStatus] = useState('loading')
  const [actionMessage, setActionMessage] = useState('')
  const [actionOk, setActionOk] = useState(false)
  const [slotTime, setSlotTime] = useState('')
  const [bookingVisit, setBookingVisit] = useState(false)

  useEffect(() => {
    fetchListing(id)
      .then((data) => {
        setListing(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [id])

  function showResult(ok, message) {
    setActionOk(ok)
    setActionMessage(message)
  }

  async function handleEnquiry() {
    setActionMessage('')
    try {
      await createEnquiry(id)
      showResult(true, "Sent! The owner will see you're interested.")
    } catch (err) {
      showResult(false, err.message)
    }
  }

  async function handleBookVisit(e) {
    e.preventDefault()
    setActionMessage('')
    if (!slotTime) {
      showResult(false, 'Please pick a date and time for your visit.')
      return
    }
    // Checked again here (not just via the input's min) because the page may
    // have been open for a while since the min was set.
    if (!isFutureIst(slotTime)) {
      showResult(false, 'Please choose a date and time in the future.')
      return
    }
    setBookingVisit(true)
    try {
      await bookVisit(id, istInputToInstant(slotTime))
      showResult(true, 'Visit requested. You will be contacted to confirm the slot.')
    } catch (err) {
      showResult(false, err.message)
    } finally {
      setBookingVisit(false)
    }
  }

  if (status === 'loading') {
    return (
      <div className="container page">
        <p className="empty-state">Loading…</p>
      </div>
    )
  }

  if (status === 'error' || !listing) {
    return (
      <div className="container page">
        <Link to="/" className="back-link">
          <ArrowLeftIcon />
          Back to listings
        </Link>
        <div className="card no-results">
          <h2>Listing not found</h2>
          <p className="text-muted">
            It may have been removed, or the link is wrong.
          </p>
          <Link to="/" className="btn btn-primary">
            Browse all listings
          </Link>
        </div>
      </div>
    )
  }

  const isPg = listing.type === 'PG'

  return (
    <div className="container page">
      <Link to="/" className="back-link">
        <ArrowLeftIcon />
        Back to listings
      </Link>

      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-media">
            <HomeIcon />
            <span className="badge badge-type badge-overlay">
              {isPg ? 'PG / Co-living' : 'For rent'}
            </span>
            {listing.verified ? (
              <span className="badge badge-verified badge-overlay badge-right">
                <CheckIcon />
                Verified
              </span>
            ) : (
              <span className="badge badge-pending badge-overlay badge-right">
                Pending verification
              </span>
            )}
          </div>

          <h1 className="detail-title">{listing.title}</h1>
          <p className="listing-meta text-muted detail-locality">
            <PinIcon />
            {listing.locality}
          </p>

          <div className="card detail-facts">
            <div className="fact">
              <span className="fact-label">Type</span>
              <span className="fact-value">{typeLabel(listing.type)}</span>
            </div>
            {listing.bedrooms != null && (
              <div className="fact">
                <span className="fact-label">Bedrooms</span>
                <span className="fact-value">
                  <BedIcon />
                  {bedroomsLabel(listing.bedrooms)}
                </span>
              </div>
            )}
            <div className="fact">
              <span className="fact-label">Locality</span>
              <span className="fact-value">{listing.locality}</span>
            </div>
          </div>

          <div className={listing.verified ? 'trust-note' : 'trust-note is-pending'}>
            <ShieldIcon />
            <p>
              {listing.verified ? (
                <>
                  <strong>Verified by our field team.</strong> This property
                  was physically visited and photographed, and listed directly
                  by the owner - no broker.
                </>
              ) : (
                <>
                  <strong>Awaiting verification.</strong> Our field team has not
                  visited this property yet, so details may change.
                </>
              )}
            </p>
          </div>
        </div>

        <aside className="card detail-side">
          <p className="price detail-price">
            ₹{formatRent(listing.rentAmount)}
            <span className="price-unit"> / month</span>
          </p>

          {!isLoggedIn ? (
            <div className="stack">
              <p className="text-muted text-sm">
                Log in to tell the owner you&apos;re interested or to book a visit.
              </p>
              <Link to="/login" className="btn btn-primary btn-block">
                Log in
              </Link>
              <Link to="/register" className="btn btn-block">
                Create an account
              </Link>
            </div>
          ) : (
            <div className="stack">
              <button
                type="button"
                className="btn btn-primary btn-block"
                onClick={handleEnquiry}
              >
                I&apos;m interested
              </button>

              <div className="side-divider" />

              <form className="stack" onSubmit={handleBookVisit} noValidate>
                <div className="field">
                  <label htmlFor="slotTime">Book a visit</label>
                  <input
                    id="slotTime"
                    type="datetime-local"
                    value={slotTime}
                    min={nowIstForInput()}
                    onChange={(e) => setSlotTime(e.target.value)}
                  />
                  <span className="field-hint text-muted">
                    Times are in India Standard Time (IST).
                  </span>
                </div>
                <button type="submit" className="btn btn-block" disabled={bookingVisit}>
                  {bookingVisit ? 'Requesting…' : 'Request visit'}
                </button>
              </form>

              {actionMessage && (
                <p
                  className={actionOk ? 'success-box' : 'error-box'}
                  role="status"
                >
                  {actionMessage}
                </p>
              )}
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
