export const STATUS_META = {
  DRAFT: { label: 'Bản nháp', tone: 'neutral' },
  PUBLISHED: { label: 'Đã xuất bản', tone: 'success' },
  UNPUBLISHED: { label: 'Đã ẩn', tone: 'muted' },
  ARCHIVED: { label: 'Lưu trữ', tone: 'dark' },
}

export const STATUS_OPTIONS = Object.entries(STATUS_META).map(([value, meta]) => ({ value, label: meta.label }))

/** Nhãn nút chuyển trạng thái, phụ thuộc trạng thái hiện tại. */
export function transitionLabel(from, to) {
  switch (to) {
    case 'PUBLISHED':
      return from === 'UNPUBLISHED' ? 'Xuất bản lại' : 'Xuất bản'
    case 'UNPUBLISHED':
      return 'Ẩn khoá học'
    case 'ARCHIVED':
      return 'Lưu trữ'
    case 'DRAFT':
      return from === 'ARCHIVED' ? 'Khôi phục' : 'Chuyển về nháp'
    default:
      return to
  }
}

/** Các bước cần xác nhận trước khi chuyển. */
export const CONFIRM_TRANSITIONS = new Set(['ARCHIVED', 'UNPUBLISHED'])

export const DELETABLE_STATUSES = new Set(['DRAFT', 'ARCHIVED'])

export const LESSON_TYPES = [
  { value: 'VIDEO', label: 'Video', icon: '▶' },
  { value: 'ARTICLE', label: 'Bài đọc', icon: '📄' },
  { value: 'QUIZ', label: 'Quiz', icon: '✔' },
]

export const MATERIAL_TYPES = [
  { value: 'VIDEO', label: 'Video' },
  { value: 'DOCUMENT', label: 'Tài liệu' },
  { value: 'IMAGE', label: 'Hình ảnh' },
  { value: 'OTHER', label: 'Khác' },
]

export function lessonTypeMeta(type) {
  return LESSON_TYPES.find((item) => item.value === type) ?? { label: type, icon: '•' }
}

export function formatBytes(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  let value = bytes / 1024
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(value < 10 ? 1 : 0)} ${units[unit]}`
}

export function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
