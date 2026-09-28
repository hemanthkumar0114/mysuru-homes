// Shimmering placeholder blocks shown instead of "Loading…" text while data
// is on its way. Purely decorative, so screen readers get one polite
// announcement instead of a wall of empty boxes.
export function ListingGridSkeleton({ count = 6 }) {
  return (
    <div className="skeleton-grid" aria-hidden="true">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="card skeleton-card">
          <div className="skeleton skeleton-media" />
          <div className="skeleton-card-body">
            <div className="skeleton skeleton-line lg" />
            <div className="skeleton skeleton-line" />
            <div className="skeleton skeleton-line w-60" />
          </div>
        </div>
      ))}
    </div>
  )
}

export function BlockSkeleton({ lines = 3 }) {
  return (
    <div className="card skeleton-block" aria-hidden="true">
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className={`skeleton skeleton-line ${i === lines - 1 ? 'w-40' : ''}`} />
      ))}
    </div>
  )
}

export function DetailSkeleton() {
  return (
    <div className="detail-layout" aria-hidden="true">
      <div className="detail-main">
        <div className="skeleton" style={{ height: 320, borderRadius: 'var(--radius-lg)' }} />
        <div className="stack" style={{ marginTop: 'var(--space-5)' }}>
          <div className="skeleton skeleton-line lg" />
          <div className="skeleton skeleton-line w-60" />
        </div>
      </div>
      <div className="card skeleton-block">
        <div className="skeleton skeleton-line lg" />
        <div className="skeleton skeleton-line" />
        <div className="skeleton skeleton-line w-60" />
      </div>
    </div>
  )
}

// A visually-hidden text alternative for screen reader / assistive tech users
// while a skeleton (aria-hidden) is showing.
export function LoadingAnnouncement({ label = 'Loading…' }) {
  return (
    <span
      role="status"
      style={{
        position: 'absolute',
        width: 1,
        height: 1,
        overflow: 'hidden',
        clip: 'rect(0 0 0 0)',
      }}
    >
      {label}
    </span>
  )
}
