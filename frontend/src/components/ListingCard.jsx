import { Link } from 'react-router-dom'
import { resolveMediaUrl } from '../api/client'
import { bedroomsLabel, formatRent } from '../utils/format'
import { BedIcon, CheckIcon, HomeIcon, PinIcon } from './icons'

export default function ListingCard({ listing }) {
  const isPg = listing.type === 'PG'
  const beds = listing.bedrooms
  const photo = resolveMediaUrl(listing.photoUrls?.[0])

  return (
    <Link to={`/listings/${listing.id}`} className="card listing-card">
      <div className="listing-card-media">
        {photo ? <img src={photo} alt="" className="listing-card-photo" /> : <HomeIcon />}
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
            Pending
          </span>
        )}
      </div>

      <div className="listing-card-body">
        <p className="price">
          ₹{formatRent(listing.rentAmount)}
          <span className="price-unit"> / month</span>
        </p>

        <h3 className="listing-title">{listing.title}</h3>

        <p className="listing-meta text-muted">
          <PinIcon />
          {listing.locality}
        </p>

        {beds != null && (
          <div className="listing-chips">
            <span className="chip">
              <BedIcon />
              {bedroomsLabel(beds)}
            </span>
          </div>
        )}
      </div>
    </Link>
  )
}
