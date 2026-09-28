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

export const HomeIcon = () => (
  <Icon>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V21h14V9.5" />
    <path d="M10 21v-6h4v6" />
  </Icon>
)

export const PinIcon = () => (
  <Icon>
    <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Icon>
)

export const BedIcon = () => (
  <Icon>
    <path d="M3 18V7" />
    <path d="M3 14h18v4" />
    <path d="M21 14v-2a3 3 0 0 0-3-3h-7v5" />
    <circle cx="7" cy="11" r="1.5" />
  </Icon>
)

export const CheckIcon = () => (
  <Icon>
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </Icon>
)

export const ArrowLeftIcon = () => (
  <Icon>
    <path d="M19 12H5" />
    <path d="m12 19-7-7 7-7" />
  </Icon>
)

export const ShieldIcon = () => (
  <Icon>
    <path d="M12 3 4.5 6v5.5c0 4.5 3.2 8.2 7.5 9.5 4.3-1.3 7.5-5 7.5-9.5V6L12 3Z" />
    <path d="m9 12 2.2 2.2L15.5 10" />
  </Icon>
)

export const SearchIcon = () => (
  <Icon>
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.3-4.3" />
  </Icon>
)

export const CalendarIcon = () => (
  <Icon>
    <rect x="3.5" y="5" width="17" height="16" rx="2" />
    <path d="M3.5 10h17" />
    <path d="M8 3v4" />
    <path d="M16 3v4" />
  </Icon>
)

export const KeyIcon = () => (
  <Icon>
    <circle cx="8" cy="15" r="4.5" />
    <path d="m11.5 11.5 8-8" />
    <path d="m16.5 6.5 3 3" />
    <path d="m14 9 3 3" />
  </Icon>
)

export const CameraIcon = () => (
  <Icon>
    <path d="M4 8h3l1.5-2h7L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1Z" />
    <circle cx="12" cy="13.5" r="3.5" />
  </Icon>
)

export const NoBrokerIcon = () => (
  <Icon>
    <circle cx="9" cy="8" r="3" />
    <path d="M3.5 20c0-3.6 2.5-6 5.5-6s5.5 2.4 5.5 6" />
    <path d="M15.5 4.5 20.5 19.5" strokeWidth="2.4" />
  </Icon>
)
