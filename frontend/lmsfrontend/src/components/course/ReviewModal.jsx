import { useState } from 'react'
import { reviewApi } from '../../api/reviewApi.js'

const STARS = [1, 2, 3, 4, 5]
const STAR_LABEL = {
  1: 'Rất tệ',
  2: 'Không hài lòng',
  3: 'Bình thường',
  4: 'Hài lòng',
  5: 'Tuyệt vời',
}

/**
 * UC-R01/R02 — gửi hoặc sửa đánh giá khoá học đã hoàn thành.
 * `existingReview` (nếu có) chuyển sang chế độ cập nhật và gọi PUT thay vì POST.
 */
export default function ReviewModal({ courseTitle, courseId, existingReview, onSaved, onCancel }) {
  const editing = Boolean(existingReview?.id)
  const [rating, setRating] = useState(existingReview?.rating ?? 5)
  const [hovered, setHovered] = useState(0)
  const [comment, setComment] = useState(existingReview?.comment ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const payload = { rating, comment: comment.trim() ? comment.trim() : null }
      const saved = editing
        ? await reviewApi.update(existingReview.id, payload)
        : await reviewApi.create(courseId, payload)
      onSaved(saved)
    } catch (requestError) {
      setError(requestError)
    } finally {
      setBusy(false)
    }
  }

  const shown = hovered || rating

  return (
    <div className="modal-backdrop" role="presentation">
      <form
        className="modal modal--small review-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-modal-title"
        onSubmit={submit}
      >
        <header className="modal__header">
          <h2 id="review-modal-title">{editing ? 'Sửa đánh giá' : 'Gửi đánh giá khoá học'}</h2>
          <p className="modal__subtitle">{courseTitle}</p>
        </header>

        <div className="modal__body">
          {error && (
            <div className="user-alert user-alert--error" role="alert">
              {error.message}
            </div>
          )}

          <fieldset className="review-stars">
            <legend>Chất lượng khoá học</legend>
            <div className="review-stars__row" onMouseLeave={() => setHovered(0)}>
              {STARS.map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`review-stars__star${value <= shown ? ' is-active' : ''}`}
                  aria-label={`${value} sao`}
                  aria-pressed={rating === value}
                  onMouseEnter={() => setHovered(value)}
                  onFocus={() => setHovered(value)}
                  onBlur={() => setHovered(0)}
                  onClick={() => setRating(value)}
                  disabled={busy}
                >
                  ★
                </button>
              ))}
              <span className="review-stars__label">
                <strong>{rating}/5</strong> · {STAR_LABEL[rating]}
              </span>
            </div>
          </fieldset>

          <label className="review-comment">
            <span>Nhận xét của bạn</span>
            <textarea
              rows={5}
              maxLength={2000}
              placeholder="Bạn thấy khoá học thế nào? Nội dung, giảng viên, bài quiz…"
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              disabled={busy}
            />
            <small className="muted">{comment.length}/2000 ký tự</small>
          </label>
        </div>

        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={onCancel} disabled={busy}>
            Huỷ
          </button>
          <button type="submit" className="button button--primary" disabled={busy}>
            {busy ? 'Đang gửi…' : editing ? 'Lưu đánh giá' : 'Gửi đánh giá'}
          </button>
        </footer>
      </form>
    </div>
  )
}
