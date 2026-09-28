// Decorative, brand-coloured SVG patterns (no stock imagery). The repeating
// scalloped arch motif nods to Mysuru's palace architecture without trying
// to be a literal illustration. Purely decorative, so it's aria-hidden.
// `id` must be unique per pattern on the page (SVG <pattern> ids are global).
export function ArchPattern({ id }) {
  return (
    <svg viewBox="0 0 320 160" preserveAspectRatio="xMidYMax slice" aria-hidden="true">
      <defs>
        <pattern id={id} width="80" height="80" patternUnits="userSpaceOnUse">
          <path
            d="M0 80 V40 a40 40 0 0 1 80 0 V80 Z"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
          />
        </pattern>
      </defs>
      <rect width="320" height="160" fill={`url(#${id})`} />
    </svg>
  )
}
