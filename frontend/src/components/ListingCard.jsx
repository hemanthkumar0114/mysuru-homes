import { Link } from 'react-router-dom'

export default function ListingCard({ listing }) {
  return (
    <Link to={`/listings/${listing.id}`} className="card listing-card">
      <h3>{listing.title}</h3>
      <p className="text-muted text-sm">
        {listing.locality} · {listing.type === 'PG' ? 'PG / Co-living' : 'Rental'}
      </p>
      <p className="price">₹{Number(listing.rentAmount).toLocaleString('en-IN')}/mo</p>
      {listing.verified ? (
        <span className="badge badge-verified">Verified</span>
      ) : (
        <span className="badge badge-pending">Pending verification</span>
      )}
    </Link>
  )
}
