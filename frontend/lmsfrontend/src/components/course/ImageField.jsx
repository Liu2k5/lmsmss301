import { useEffect, useMemo } from 'react'
import { formatBytes } from './courseMeta.js'

/** Chọn ảnh từ máy, xem trước ngay, cho phép bỏ ảnh. */
export default function ImageField({ id, value, disabled, onChange }) {
  const previewUrl = useMemo(() => (value.file ? URL.createObjectURL(value.file) : null), [value.file])

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
  }, [previewUrl])

  const shown = previewUrl ?? (value.removed ? null : value.url)

  return (
    <div className="image-field">
      <div className="image-field__preview">
        {shown ? <img src={shown} alt="Xem trước ảnh bìa" /> : <span className="muted small">Chưa có ảnh bìa</span>}
      </div>
      <div className="image-field__actions">
        <input
          id={id}
          type="file"
          accept="image/*"
          className="visually-hidden"
          disabled={disabled}
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) onChange({ ...value, file, removed: false })
            event.target.value = ''
          }}
        />
        <label htmlFor={id} className={`button button--ghost button--small${disabled ? ' is-disabled' : ''}`}>
          {shown ? 'Đổi ảnh' : 'Chọn ảnh từ máy'}
        </label>
        {shown && (
          <button
            type="button"
            className="button button--danger-ghost button--small"
            disabled={disabled}
            onClick={() => onChange({ ...value, file: null, removed: true })}
          >
            Bỏ ảnh
          </button>
        )}
        {value.file && (
          <span className="muted small image-field__name">
            {value.file.name} · {formatBytes(value.file.size)}
          </span>
        )}
      </div>
    </div>
  )
}
