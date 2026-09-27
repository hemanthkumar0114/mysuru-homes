import { useEffect, useState } from 'react'
import ListingCard from '../components/ListingCard'
import { fetchListings } from '../api/client'

export default function Home() {
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('loading')
  const [locality, setLocality] = useState('')

  useEffect(() => {
    setStatus('loading')
    fetchListings(locality ? { locality } : {})
      .then((data) => {
        setListings(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [locality])

  return (
    <>
      <section className="hero">
        <div className="container center-text">
          <h1>Verified rentals in Mysuru, owner-direct.</h1>
          <p className="subtitle" style={{ margin: '0 auto' }}>
            Every listing is physically visited and photographed by our team.
            No broker spam.
          </p>
        </div>
      </section>

      <div className="container page">
      <div className="field" style={{ maxWidth: 320, margin: '0 auto 32px' }}>
        <label htmlFor="locality">Filter by locality</label>
        <input
          id="locality"
          placeholder="e.g. Vijayanagar, Hebbal, Bogadi"
          value={locality}
          onChange={(e) => setLocality(e.target.value)}
        />
      </div>

      {status === 'loading' && <p className="empty-state">Loading listings…</p>}

      {status === 'error' && (
        <p className="empty-state text-danger">
          Could not reach the API. Is the backend running?
        </p>
      )}

      {status === 'ready' && listings.length === 0 && (
        <p className="empty-state">No live listings match yet.</p>
      )}

      {status === 'ready' && listings.length > 0 && (
        <div className="grid">
          {listings.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </div>
      )}
      </div>
    </>
  )
}
