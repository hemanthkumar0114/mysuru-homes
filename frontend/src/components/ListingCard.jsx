import { Link } from 'react-router-dom'

// Small inline SVG icons (no icon library needed). aria-hidden because the
// text next to each one already says what it is.
function Icon({ children }) {
  return (
    <svg
      className="icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

const HomeIcon = () => (
  <Icon>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V21h14V9.5" />
    <path d="M10 21v-6h4v6" />
  </Icon>
)

const PinIcon = () => (
  <Icon>
    <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Icon>
)

const BedIcon = () => (
  <Icon>
    <path d="M3 18V7" />
    <path d="M3 14h18v4" />
    <path d="M21 14v-2a3 3 0 0 0-3-3h-7v5" />
    <circle cx="7" cy="11" r="1.5" />
  </Icon>
)

const CheckIcon = () => (
  <Icon>
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </Icon>
)

export default function ListingCard({ listing }) {
  const isPg = listing.type === 'PG'
  const rent = Number(listing.rentAmount)
  const price = rent.toLocaleString('en-IN', {
    minimumFractionDigits: Number.isInteger(rent) ? 0 : 2,
    maximumFractionDigits: 2,
  })
  const beds = listing.bedrooms
  const bedsLabel = beds === 0 ? 'Studio' : `${beds} ${beds === 1 ? 'bedroom' : 'bedrooms'}`

  return (
    <Link to={`/listings/${listing.id}`} className="card listing-card">
      <div className="listing-card-media">
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
            Pending
          </span>
        )}
      </div>

      <div className="listing-card-body">
        <p className="price">
          ₹{price}
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
              {bedsLabel}
            </span>
          </div>
        )}
      </div>
    </Link>
  )
}
