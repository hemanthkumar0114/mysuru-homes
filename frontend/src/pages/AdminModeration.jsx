import { useEffect, useState } from 'react'
import { fetchPendingListings, verifyListing } from '../api/client'

export default function AdminModeration() {
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('loading')
  const [busyId, setBusyId] = useState(null)

  function load() {
    setStatus('loading')
    fetchPendingListings()
      .then((data) => {
        setListings(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(load, [])

  async function handleVerify(id) {
    setBusyId(id)
    try {
      await verifyListing(id)
      setListings((prev) => prev.filter((l) => l.id !== id))
    } catch (err) {
      alert(err.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="container page">
      <h1>Listings pending verification</h1>
      <p className="text-muted">
        Confirm the field team has physically visited and photographed each
        listing before marking it verified and live.
      </p>
      <div className="spacer-md" />

      {status === 'loading' && <p className="empty-state">Loading…</p>}
      {status === 'error' && (
        <p className="empty-state text-danger">Could not load pending listings.</p>
      )}
      {status === 'ready' && listings.length === 0 && (
        <p className="empty-state">Nothing pending. All caught up.</p>
      )}

      {status === 'ready' && listings.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Locality</th>
              <th>Owner</th>
              <th>Rent</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {listings.map((listing) => (
              <tr key={listing.id}>
                <td>{listing.title}</td>
                <td>{listing.locality}</td>
                <td>{listing.ownerName}</td>
                <td>₹{Number(listing.rentAmount).toLocaleString('en-IN')}/mo</td>
                <td>
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={busyId === listing.id}
                    onClick={() => handleVerify(listing.id)}
                  >
                    {busyId === listing.id ? 'Verifying…' : 'Mark verified'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
