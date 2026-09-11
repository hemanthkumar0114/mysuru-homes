import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { bookVisit, createEnquiry, fetchListing } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function ListingDetail() {
  const { id } = useParams()
  const { isLoggedIn } = useAuth()

  const [listing, setListing] = useState(null)
  const [status, setStatus] = useState('loading')
  const [actionMessage, setActionMessage] = useState('')
  const [slotTime, setSlotTime] = useState('')

  useEffect(() => {
    fetchListing(id)
      .then((data) => {
        setListing(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [id])

  async function handleEnquiry() {
    setActionMessage('')
    try {
      await createEnquiry(id)
      setActionMessage("Sent! The owner will see you're interested.")
    } catch (err) {
      setActionMessage(err.message)
    }
  }

  async function handleBookVisit(e) {
    e.preventDefault()
    setActionMessage('')
    try {
      await bookVisit(id, slotTime)
      setActionMessage('Visit requested. You will be contacted to confirm the slot.')
    } catch (err) {
      setActionMessage(err.message)
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
        <p className="empty-state text-danger">Listing not found.</p>
      </div>
    )
  }

  return (
    <div className="container page">
      <h1>{listing.title}</h1>
      <p className="text-muted">
        {listing.locality} · {listing.type === 'PG' ? 'PG / Co-living' : 'Rental'}
      </p>

      {listing.verified ? (
        <span className="badge badge-verified">Verified by our field team</span>
      ) : (
        <span className="badge badge-pending">Pending verification</span>
      )}

      <div className="spacer-md" />

      <div className="card" style={{ maxWidth: 480 }}>
        <p className="price" style={{ fontSize: '1.4rem' }}>
          ₹{Number(listing.rentAmount).toLocaleString('en-IN')}/mo
        </p>
        {listing.bedrooms != null && (
          <p className="text-muted text-sm">{listing.bedrooms} bedroom(s)</p>
        )}
      </div>

      <div className="spacer-lg" />

      {!isLoggedIn && (
        <p className="text-muted">
          <Link to="/login">Log in</Link> to enquire or book a visit.
        </p>
      )}

      {isLoggedIn && (
        <div className="stack" style={{ maxWidth: 420 }}>
          <button type="button" className="btn btn-primary" onClick={handleEnquiry}>
            I'm interested
          </button>

          <form className="stack" onSubmit={handleBookVisit}>
            <div className="field">
              <label htmlFor="slotTime">Book a visit</label>
              <input
                id="slotTime"
                type="datetime-local"
                value={slotTime}
                onChange={(e) => setSlotTime(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn">
              Request visit
            </button>
          </form>

          {actionMessage && <p className="text-sm">{actionMessage}</p>}
        </div>
      )}
    </div>
  )
}
