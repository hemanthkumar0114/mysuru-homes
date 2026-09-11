import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchMyListings } from '../api/client'

const STATUS_LABEL = {
  DRAFT: 'Pending verification',
  LIVE: 'Live',
  EXPIRED: 'Expired',
}

export default function MyListings() {
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    fetchMyListings()
      .then((data) => {
        setListings(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [])

  return (
    <div className="container page">
      <div className="row-between">
        <h1>My listings</h1>
        <Link to="/post-property" className="btn btn-primary">
          Post a property
        </Link>
      </div>
      <div className="spacer-md" />

      {status === 'loading' && <p className="empty-state">Loading…</p>}
      {status === 'error' && (
        <p className="empty-state text-danger">Could not load your listings.</p>
      )}
      {status === 'ready' && listings.length === 0 && (
        <p className="empty-state">You haven't posted any properties yet.</p>
      )}

      {status === 'ready' && listings.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Locality</th>
              <th>Rent</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {listings.map((listing) => (
              <tr key={listing.id}>
                <td>{listing.title}</td>
                <td>{listing.locality}</td>
                <td>₹{Number(listing.rentAmount).toLocaleString('en-IN')}/mo</td>
                <td>{STATUS_LABEL[listing.status] || listing.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
