// The <input type="datetime-local"> gives wall-clock text like "2026-09-27T09:42"
// (no seconds, no time zone). Visits are at physical properties in Mysuru, so
// that text always means India Standard Time - not the visitor's device zone.
// India has no daylight saving, so the fixed +05:30 offset is always exact.

const IST_OFFSET = '+05:30'
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000

function withSeconds(value) {
  return value.length === 16 ? `${value}:00` : value
}

function istInputToDate(value) {
  return new Date(`${withSeconds(value)}${IST_OFFSET}`)
}

// "2026-09-27T09:42" (IST) -> "2026-09-27T04:12:00.000Z": an exact instant the
// backend's Instant field accepts.
export function istInputToInstant(value) {
  return istInputToDate(value).toISOString()
}

export function isFutureIst(value) {
  const time = istInputToDate(value).getTime()
  return !Number.isNaN(time) && time > Date.now()
}

// "Now" as IST wall-clock text, for the input's min attribute.
export function nowIstForInput() {
  return new Date(Date.now() + IST_OFFSET_MS).toISOString().slice(0, 16)
}

// "2026-09-27T04:12:00Z" -> "27 Sept 2026, 9:42 am IST": a moment in India time,
// whatever time zone the visitor's device is set to.
export function formatIstDateTime(isoInstant) {
  const text = new Date(isoInstant).toLocaleString('en-IN', {
    timeZone: 'Asia/Kolkata',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  })
  return `${text} IST`
}

// "2026-09-11T06:26:05Z" -> "11 Sept 2026", shown in India time.
export function formatIstDate(isoInstant) {
  return new Date(isoInstant).toLocaleDateString('en-IN', {
    timeZone: 'Asia/Kolkata',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}
