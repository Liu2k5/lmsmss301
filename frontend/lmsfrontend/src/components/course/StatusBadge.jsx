import { STATUS_META } from './courseMeta.js'

export default function StatusBadge({ status }) {
  const meta = STATUS_META[status] ?? { label: status, tone: 'neutral' }
  return <span className={`status status--${meta.tone}`}>{meta.label}</span>
}
