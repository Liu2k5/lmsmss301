import { useState } from 'react'
import { reviewApi } from '../../api/reviewApi.js'

const REASON_PRESETS = [
  'Spam hoặc quảng cáo không liên quan',
  'Ngôn từ xúc phạm, quấy rối hoặc thù ghét',
  'Thông tin sai lệch hoặc cố tình phá hoại',
  'Khác (vui lòng nêu rõ bên dưới)',
]

export default function ReportModal({ reviewId, onReported, onCancel }) {
  const [selectedPreset, setSelectedPreset] = useState(REASON_PRESETS[0])
  const [customReason, setCustomReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const reasonText =
      selectedPreset === REASON_PRESETS[3] || customReason.trim()
        ? customReason.trim()
          ? `${selectedPreset}: ${customReason.trim()}`
          : selectedPreset
        : selectedPreset

    try {
      await reviewApi.report(reviewId, { reason: reasonText })
      onReported()
    } catch (requestError) {
      if (requestError.status === 409) {
        setError(new Error('Bạn đã gửi báo cáo cho đánh giá này rồi.'))
      } else {
        setError(requestError)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <form
        className="modal modal--small report-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="report-modal-title"
        onSubmit={submit}
      >
        <header className="modal__header">
          <h2 id="report-modal-title">Báo cáo đánh giá vi phạm</h2>
          <p className="modal__subtitle">Giúp ban quản trị kiểm tra và giữ gìn môi trường học tập lành mạnh.</p>
        </header>

        <div className="modal__body">
          {error && (
            <div className="user-alert user-alert--error" role="alert">
              {error.message}
            </div>
          )}

          <fieldset className="report-presets">
            <legend className="form-field__label">Lý do báo cáo</legend>
            <div className="report-presets__list">
              {REASON_PRESETS.map((preset) => (
                <label key={preset} className="report-preset__item">
                  <input
                    type="radio"
                    name="reportPreset"
                    value={preset}
                    checked={selectedPreset === preset}
                    onChange={() => setSelectedPreset(preset)}
                    disabled={busy}
                  />
                  <span>{preset}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <label className="form-field">
            <span className="form-field__label">Ghi chú thêm (không bắt buộc)</span>
            <textarea
              rows={3}
              maxLength={500}
              placeholder="Cung cấp thêm chi tiết giúp chúng tôi xử lý nhanh hơn…"
              value={customReason}
              onChange={(e) => setCustomReason(e.target.value)}
              disabled={busy}
            />
            <small className="muted">{customReason.length}/500 ký tự</small>
          </label>
        </div>

        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={busy}>
            Huỷ
          </button>
          <button type="submit" className="button button--danger" disabled={busy}>
            {busy ? 'Đang gửi…' : 'Gửi báo cáo'}
          </button>
        </footer>
      </form>
    </div>
  )
}
