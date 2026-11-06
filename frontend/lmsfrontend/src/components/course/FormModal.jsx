import { useEffect, useRef, useState } from 'react'
import ImageField from './ImageField.jsx'
import { initialImageValue, validateImage } from './imageValue.js'

/**
 * Modal form dùng chung cho khoá học / chương / bài học / học liệu.
 *
 * fields: [{ name, label, type: 'text'|'url'|'number'|'textarea'|'select'|'checkbox'|'image',
 *            required, maxLength, maxBytes, options, hint, wide, placeholder }]
 * Field 'image' nhận initialValues[name] là URL ảnh hiện tại và trả về payload[name] = { file, removed }.
 * onSubmit(values) trả về Promise; lỗi ApiError sẽ được hiển thị (kèm lỗi theo field).
 */
export default function FormModal({ title, subtitle, fields, initialValues, submitLabel = 'Lưu', onSubmit, onCancel }) {
  const [values, setValues] = useState(() => {
    const start = {}
    fields.forEach((field) => {
      const initial = initialValues?.[field.name]
      if (field.type === 'image') {
        start[field.name] = initialImageValue(initial)
      } else {
        start[field.name] = field.type === 'checkbox' ? Boolean(initial) : (initial ?? '')
      }
    })
    return start
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [saving, setSaving] = useState(false)
  const firstRef = useRef(null)

  useEffect(() => {
    firstRef.current?.focus()
  }, [])

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !saving) {
        onCancel()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onCancel, saving])

  function setValue(name, value) {
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  function validate() {
    const errors = {}
    fields.forEach((field) => {
      const value = values[field.name]
      if (field.type === 'image') {
        const imageError = validateImage(value, field.maxBytes)
        if (imageError) errors[field.name] = imageError
      } else if (field.required && field.type !== 'checkbox' && String(value).trim() === '') {
        errors[field.name] = `${field.label} không được để trống.`
      } else if (field.type === 'url' && String(value).trim() && !/^https?:\/\/.+/.test(String(value).trim())) {
        errors[field.name] = 'URL phải bắt đầu bằng http:// hoặc https://'
      } else if (field.type === 'number' && value !== '' && (Number.isNaN(Number(value)) || Number(value) < 0)) {
        errors[field.name] = `${field.label} phải là số không âm.`
      }
    })
    return errors
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    setServerError(null)
    if (Object.keys(errors).length > 0) {
      return
    }

    const payload = {}
    fields.forEach((field) => {
      const value = values[field.name]
      if (field.type === 'image') {
        payload[field.name] = { file: value.file, removed: value.removed && Boolean(value.url) }
      } else if (field.type === 'checkbox') {
        payload[field.name] = Boolean(value)
      } else if (field.type === 'number') {
        payload[field.name] = value === '' ? null : Number(value)
      } else {
        const trimmed = String(value).trim()
        payload[field.name] = trimmed === '' ? null : trimmed
      }
    })

    setSaving(true)
    try {
      await onSubmit(payload)
    } catch (error) {
      const byField = {}
      ;(error.details ?? []).forEach((detail) => {
        byField[detail.field] = detail.message
      })
      setFieldErrors(byField)
      setServerError(error.message)
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <form className="modal modal--medium" role="dialog" aria-modal="true" aria-labelledby="form-modal-title"
        onSubmit={handleSubmit} noValidate>
        <header className="modal__header">
          <div>
            <h2 id="form-modal-title">{title}</h2>
            {subtitle && <p className="modal__subtitle">{subtitle}</p>}
          </div>
          <button type="button" className="icon-button" aria-label="Đóng" onClick={onCancel} disabled={saving}>
            ✕
          </button>
        </header>

        <div className="modal__body">
          {serverError && (
            <div className="alert alert--error" role="alert">
              <strong>{serverError}</strong>
            </div>
          )}

          <div className="field-grid">
            {fields.map((field, index) => {
              const id = `field-${field.name}`
              const error = fieldErrors[field.name]
              const common = {
                id,
                ref: index === 0 ? firstRef : undefined,
                'aria-invalid': Boolean(error),
                disabled: saving,
              }

              if (field.type === 'checkbox') {
                return (
                  <label key={field.name} className="field field--wide field--checkbox" htmlFor={id}>
                    <input
                      {...common}
                      type="checkbox"
                      checked={values[field.name]}
                      onChange={(event) => setValue(field.name, event.target.checked)}
                    />
                    <span>
                      <span className="field__label">{field.label}</span>
                      {field.hint && <span className="field__hint"> — {field.hint}</span>}
                    </span>
                  </label>
                )
              }

              let control
              if (field.type === 'image') {
                control = (
                  <ImageField
                    id={id}
                    value={values[field.name]}
                    disabled={saving}
                    onChange={(next) => setValue(field.name, next)}
                  />
                )
              } else if (field.type === 'textarea') {
                control = (
                  <textarea
                    {...common}
                    rows={4}
                    maxLength={field.maxLength}
                    placeholder={field.placeholder}
                    value={values[field.name]}
                    onChange={(event) => setValue(field.name, event.target.value)}
                  />
                )
              } else if (field.type === 'select') {
                control = (
                  <select {...common} value={values[field.name]}
                    onChange={(event) => setValue(field.name, event.target.value)}>
                    {field.options.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                )
              } else {
                control = (
                  <input
                    {...common}
                    type={field.type === 'number' ? 'number' : 'text'}
                    inputMode={field.type === 'url' ? 'url' : undefined}
                    min={field.type === 'number' ? 0 : undefined}
                    maxLength={field.maxLength}
                    placeholder={field.placeholder}
                    value={values[field.name]}
                    onChange={(event) => setValue(field.name, event.target.value)}
                  />
                )
              }

              return (
                <div key={field.name} className={`field${field.wide ? ' field--wide' : ''}`}>
                  <label className="field__label" htmlFor={id}>
                    {field.label}
                    {field.required && <span className="field__required"> *</span>}
                  </label>
                  {control}
                  {error ? <span className="field__error">{error}</span>
                    : field.hint && <span className="field__hint">{field.hint}</span>}
                </div>
              )
            })}
          </div>
        </div>

        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={saving}>
            Huỷ
          </button>
          <button type="submit" className="button button--primary" disabled={saving}>
            {saving ? 'Đang lưu…' : submitLabel}
          </button>
        </footer>
      </form>
    </div>
  )
}
