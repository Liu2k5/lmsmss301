import { useEffect, useRef, useState } from 'react'
import { MATERIAL_TYPES, formatBytes } from './courseMeta.js'

function guessType(file) {
  if (!file?.type) return ''
  if (file.type.startsWith('video/')) return 'VIDEO'
  if (file.type.startsWith('image/')) return 'IMAGE'
  if (file.type === 'application/pdf' || file.type.startsWith('text/') || /word|presentation|sheet|excel|powerpoint/.test(file.type)) {
    return 'DOCUMENT'
  }
  return 'OTHER'
}

/**
 * Upload video / tài liệu lên MinIO. onUpload(file, { name, type }, onProgress) trả về { promise, abort }.
 */
export default function UploadModal({ subtitle, defaultType, onUpload, onCancel }) {
  const [file, setFile] = useState(null)
  const [name, setName] = useState('')
  const [type, setType] = useState(defaultType ?? '')
  const [progress, setProgress] = useState(null)
  const [error, setError] = useState(null)
  const [dragging, setDragging] = useState(false)
  const uploadRef = useRef(null)
  const inputRef = useRef(null)

  const uploading = progress !== null

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !uploading) onCancel()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onCancel, uploading])

  function pick(selected) {
    if (!selected) return
    setFile(selected)
    setError(null)
    setName(selected.name.replace(/\.[^.]+$/, ''))
    setType(guessType(selected) || defaultType || '')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!file) {
      setError('Chọn file cần upload.')
      return
    }
    setError(null)
    setProgress(0)
    const upload = onUpload(file, { name: name.trim(), type }, setProgress)
    uploadRef.current = upload
    try {
      await upload.promise
    } catch (uploadError) {
      setError(uploadError.message)
      setProgress(null)
    } finally {
      uploadRef.current = null
    }
  }

  function handleCancel() {
    if (uploadRef.current) {
      uploadRef.current.abort()
      return
    }
    onCancel()
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <form className="modal modal--medium" role="dialog" aria-modal="true" aria-labelledby="upload-title"
        onSubmit={handleSubmit} noValidate>
        <header className="modal__header">
          <div>
            <h2 id="upload-title">Upload học liệu</h2>
            {subtitle && <p className="modal__subtitle">{subtitle}</p>}
          </div>
          <button type="button" className="icon-button" aria-label="Đóng" onClick={onCancel} disabled={uploading}>
            ✕
          </button>
        </header>

        <div className="modal__body">
          {error && (
            <div className="alert alert--error" role="alert">
              <strong>{error}</strong>
            </div>
          )}

          <div
            className={`dropzone${dragging ? ' is-dragging' : ''}${file ? ' has-file' : ''}`}
            onDragOver={(event) => {
              event.preventDefault()
              if (!uploading) setDragging(true)
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(event) => {
              event.preventDefault()
              setDragging(false)
              if (!uploading) pick(event.dataTransfer.files?.[0])
            }}
          >
            <input
              ref={inputRef}
              id="upload-file"
              type="file"
              className="visually-hidden"
              accept="video/*,image/*,application/pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.zip,.txt"
              onChange={(event) => pick(event.target.files?.[0])}
              disabled={uploading}
            />
            {file ? (
              <div className="dropzone__file">
                <strong>{file.name}</strong>
                <span className="muted small">
                  {formatBytes(file.size)}
                  {file.type ? ` · ${file.type}` : ''}
                </span>
              </div>
            ) : (
              <p className="muted">Kéo thả file vào đây</p>
            )}
            <label htmlFor="upload-file" className={`button button--ghost button--small${uploading ? ' is-disabled' : ''}`}>
              {file ? 'Chọn file khác' : 'Chọn file từ máy'}
            </label>
          </div>

          <div className="field-grid">
            <div className="field field--wide">
              <label className="field__label" htmlFor="upload-name">
                Tên hiển thị
              </label>
              <input id="upload-name" type="text" maxLength={255} value={name} disabled={uploading}
                placeholder="Mặc định lấy theo tên file" onChange={(event) => setName(event.target.value)} />
            </div>
            <div className="field">
              <label className="field__label" htmlFor="upload-type">
                Loại
              </label>
              <select id="upload-type" value={type} disabled={uploading} onChange={(event) => setType(event.target.value)}>
                <option value="">Tự nhận theo file</option>
                {MATERIAL_TYPES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {uploading && (
            <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress}>
              <div className="progress__bar" style={{ width: `${progress}%` }} />
              <span className="progress__label">
                {progress < 100 ? `Đang upload… ${progress}%` : 'Đang lưu lên MinIO…'}
              </span>
            </div>
          )}
        </div>

        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={handleCancel}>
            {uploading ? 'Huỷ upload' : 'Huỷ'}
          </button>
          <button type="submit" className="button button--primary" disabled={uploading || !file}>
            {uploading ? 'Đang upload…' : 'Upload'}
          </button>
        </footer>
      </form>
    </div>
  )
}
