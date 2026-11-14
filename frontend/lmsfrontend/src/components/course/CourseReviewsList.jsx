import { useCallback, useEffect, useState } from 'react'
import { reviewApi } from '../../api/reviewApi.js'
import { formatDate } from './courseMeta.js'
import ReportModal from './ReportModal.jsx'

const SORT_OPTIONS = [
  { value: 'newest', label: 'Mới nhất' },
  { value: 'highest_rating', label: 'Đánh giá cao nhất' },
  { value: 'lowest_rating', label: 'Đánh giá thấp nhất' },
  { value: 'most_helpful', label: 'Nhiều người thấy hữu ích' },
]

export default function CourseReviewsList({
  courseId,
  activeRatingFilter,
  currentUserId,
  canManage,
  isAdmin,
  onEditReview,
  onReviewsChanged,
  notify,
}) {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [sort, setSort] = useState('newest')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Báo cáo vi phạm
  const [reportingReviewId, setReportingReviewId] = useState(null)
  // Phản hồi của giảng viên
  const [replyingReviewId, setReplyingReviewId] = useState(null)
  const [replyContent, setReplyContent] = useState('')
  const [replyBusy, setReplyBusy] = useState(false)
  // Xoá review
  const [deletingId, setDeletingId] = useState(null)
  // Toggle helpful
  const [votingId, setVotingId] = useState(null)

  const loadReviews = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        sort,
        page,
        size: 5,
      }
      if (activeRatingFilter != null) {
        params.rating = activeRatingFilter
      }
      const data = await reviewApi.listByCourse(courseId, params)
      // data là Page<ReviewResponse>: { content, totalPages, totalElements, number, ... }
      if (Array.isArray(data)) {
        setReviews(data)
        setTotalPages(1)
        setTotalElements(data.length)
      } else {
        setReviews(data?.content ?? [])
        setTotalPages(data?.totalPages ?? 0)
        setTotalElements(data?.totalElements ?? 0)
      }
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }, [courseId, activeRatingFilter, sort, page])

  useEffect(() => {
    loadReviews()
  }, [loadReviews])

  // Reset về page 0 khi đổi rating filter hoặc sort
  useEffect(() => {
    setPage(0)
  }, [activeRatingFilter, sort])

  async function handleToggleHelpful(review) {
    if (!currentUserId) {
      notify('error', 'Vui lòng đăng nhập để đánh giá nhận xét hữu ích.')
      return
    }
    setVotingId(review.id)
    try {
      const result = await reviewApi.toggleHelpful(review.id)
      setReviews((prev) =>
        prev.map((r) =>
          r.id === review.id
            ? {
                ...r,
                helpfulCount: result.helpfulCount,
                isHelpfulByMe: result.isHelpfulByMe,
              }
            : r,
        ),
      )
    } catch (err) {
      notify('error', err.message)
    } finally {
      setVotingId(null)
    }
  }

  async function handleDeleteReview(review) {
    if (!window.confirm('Bạn có chắc chắn muốn xoá đánh giá này không?')) {
      return
    }
    setDeletingId(review.id)
    try {
      await reviewApi.remove(review.id)
      notify('success', 'Đã xoá đánh giá.')
      await loadReviews()
      onReviewsChanged?.()
    } catch (err) {
      notify('error', err.message)
    } finally {
      setDeletingId(null)
    }
  }

  async function handleSendReply(reviewId) {
    if (!replyContent.trim()) {
      return
    }
    setReplyBusy(true)
    try {
      const replyData = await reviewApi.reply(reviewId, { content: replyContent.trim() })
      setReviews((prev) =>
        prev.map((r) =>
          r.id === reviewId
            ? {
                ...r,
                reply: replyData,
              }
            : r,
        ),
      )
      setReplyingReviewId(null)
      setReplyContent('')
      notify('success', 'Đã gửi phản hồi cho học viên.')
    } catch (err) {
      notify('error', err.message)
    } finally {
      setReplyBusy(false)
    }
  }

  return (
    <div className="course-reviews-list">
      <div className="course-reviews-list__header">
        <div className="course-reviews-list__title">
          <h3>Nhận xét từ học viên</h3>
          <span className="muted small">({totalElements} nhận xét)</span>
        </div>
        <div className="course-reviews-list__sort">
          <label htmlFor="review-sort" className="muted small">
            Sắp xếp theo:
          </label>
          <select
            id="review-sort"
            className="input-select"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="alert alert--error" role="alert">
          {error.message}
          <button type="button" className="button button--ghost button--small" onClick={loadReviews}>
            Thử lại
          </button>
        </div>
      )}

      {loading && (
        <div className="state-card state-card--compact">
          <span className="spinner" aria-hidden="true" />
          <p>Đang tải nhận xét…</p>
        </div>
      )}

      {!loading && reviews.length === 0 && (
        <div className="state-card state-card--compact">
          <p>
            {activeRatingFilter != null
              ? `Chưa có nhận xét nào được đánh giá ${activeRatingFilter} sao.`
              : 'Khoá học chưa có nhận xét nào. Hãy là người đầu tiên chia sẻ cảm nhận!'}
          </p>
        </div>
      )}

      {!loading && reviews.length > 0 && (
        <div className="review-cards">
          {reviews.map((review) => {
            const isMine = currentUserId && review.studentId === currentUserId
            const canDelete = isMine || isAdmin
            const isInstructorReply = Boolean(review.reply)

            return (
              <article key={review.id} className="review-card">
                <header className="review-card__header">
                  <div className="review-card__author">
                    <div className="review-card__avatar" aria-hidden="true">
                      {(isMine ? 'Bạn' : review.studentId ? review.studentId.slice(0, 2) : 'HV').toUpperCase()}
                    </div>
                    <div>
                      <div className="review-card__name">
                        <strong>{isMine ? 'Bạn' : `Học viên #${review.studentId?.slice(0, 8) ?? '...'}`}</strong>
                        {isMine && <span className="tag tag--info">Của bạn</span>}
                      </div>
                      <span className="muted small">{formatDate(review.createdAt)}</span>
                    </div>
                  </div>

                  <div className="review-card__rating">
                    <span className="review-stars-display" aria-label={`${review.rating} sao`}>
                      {'★'.repeat(review.rating)}
                      {'☆'.repeat(5 - review.rating)}
                    </span>
                    <strong className="review-stars-num">{review.rating}/5</strong>
                  </div>
                </header>

                <div className="review-card__content">
                  <p>{review.comment || <em className="muted">Không có nhận xét chi tiết.</em>}</p>
                </div>

                {/* Phản hồi từ giảng viên */}
                {isInstructorReply && (
                  <div className="review-card__reply">
                    <div className="review-card__reply-head">
                      <strong>🎓 Phản hồi từ Giảng viên</strong>
                      <span className="muted small">{formatDate(review.reply.createdAt)}</span>
                    </div>
                    <p className="review-card__reply-body">{review.reply.content}</p>
                  </div>
                )}

                {/* Form phản hồi cho giảng viên / admin */}
                {canManage && replyingReviewId === review.id && (
                  <div className="review-reply-form">
                    <textarea
                      rows={3}
                      maxLength={2000}
                      placeholder="Viết phản hồi công khai của bạn cho học viên này…"
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      disabled={replyBusy}
                    />
                    <div className="review-reply-form__actions">
                      <button
                        type="button"
                        className="button button--ghost button--small"
                        onClick={() => {
                          setReplyingReviewId(null)
                          setReplyContent('')
                        }}
                        disabled={replyBusy}
                      >
                        Huỷ
                      </button>
                      <button
                        type="button"
                        className="button button--primary button--small"
                        onClick={() => handleSendReply(review.id)}
                        disabled={replyBusy || !replyContent.trim()}
                      >
                        {replyBusy ? 'Đang gửi…' : 'Gửi phản hồi'}
                      </button>
                    </div>
                  </div>
                )}

                <footer className="review-card__footer">
                  <div className="review-card__interactions">
                    <button
                      type="button"
                      className={`button button--ghost button--small helpful-btn${review.isHelpfulByMe ? ' is-helpful' : ''}`}
                      onClick={() => handleToggleHelpful(review)}
                      disabled={votingId === review.id}
                      title="Nhận xét này hữu ích với bạn?"
                    >
                      👍 Hữu ích ({review.helpfulCount ?? 0})
                    </button>

                    <button
                      type="button"
                      className="button button--ghost button--small report-btn"
                      onClick={() => setReportingReviewId(review.id)}
                      title="Báo cáo vi phạm"
                    >
                      🚩 Báo cáo
                    </button>
                  </div>

                  <div className="review-card__manage">
                    {canManage && replyingReviewId !== review.id && (
                      <button
                        type="button"
                        className="button button--ghost button--small"
                        onClick={() => {
                          setReplyingReviewId(review.id)
                          setReplyContent(review.reply?.content ?? '')
                        }}
                      >
                        {isInstructorReply ? '✎ Sửa phản hồi' : '💬 Trả lời'}
                      </button>
                    )}

                    {isMine && onEditReview && (
                      <button
                        type="button"
                        className="button button--ghost button--small"
                        onClick={() => onEditReview(review)}
                      >
                        ✎ Sửa
                      </button>
                    )}

                    {canDelete && (
                      <button
                        type="button"
                        className="button button--danger-ghost button--small"
                        onClick={() => handleDeleteReview(review)}
                        disabled={deletingId === review.id}
                      >
                        {deletingId === review.id ? 'Đang xoá…' : '🗑 Xoá'}
                      </button>
                    )}
                  </div>
                </footer>
              </article>
            )
          })}
        </div>
      )}

      {/* Phân trang */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="button button--ghost button--small"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0 || loading}
          >
            ← Trang trước
          </button>
          <span className="pagination__info muted small">
            Trang {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="button button--ghost button--small"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1 || loading}
          >
            Trang sau →
          </button>
        </div>
      )}

      {reportingReviewId && (
        <ReportModal
          reviewId={reportingReviewId}
          onCancel={() => setReportingReviewId(null)}
          onReported={() => {
            setReportingReviewId(null)
            notify('success', 'Đã gửi báo cáo vi phạm. Cảm ơn sự đóng góp của bạn!')
          }}
        />
      )}
    </div>
  )
}
