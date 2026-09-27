import { visitStatus } from '../utils/format'

export default function VisitStatusBadge({ status }) {
  const { label, tone } = visitStatus(status)
  return <span className={`badge ${tone}`}>{label}</span>
}
