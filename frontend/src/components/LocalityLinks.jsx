import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchLocalities } from '../api/client'
import { PinIcon } from './icons'
import { pluralize, rentRangeLabel } from '../utils/format'

// "Explore by locality" cards on the home page. If the request fails the section
// simply doesn't appear - the rest of the home page works without it.
export default function LocalityLinks() {
  const [localities, setLocalities] = useState([])

  useEffect(() => {
    let ignore = false
    fetchLocalities()
      .then((data) => !ignore && setLocalities(data))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  if (localities.length === 0) return null

  return (
    <section className="locality-links" aria-labelledby="locality-links-title">
      <h2 id="locality-links-title">Explore by locality</h2>
      <p className="text-muted">Rent ranges and live listings across the Mysuru corridor.</p>
      <div className="locality-grid">
        {localities.map((locality) => (
          <Link key={locality.slug} to={`/localities/${locality.slug}`} className="card locality-card">
            <span className="locality-card-name">
              <PinIcon />
              {locality.name}
            </span>
            <span className="locality-card-rent">
              {rentRangeLabel(locality.minRent, locality.maxRent) ?? 'No live listings yet'}
            </span>
            <span className="text-muted text-sm">{pluralize(locality.listingCount, 'live listing')}</span>
          </Link>
        ))}
      </div>
    </section>
  )
}
