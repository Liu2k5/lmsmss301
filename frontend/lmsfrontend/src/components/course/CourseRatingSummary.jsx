export default function CourseRatingSummary({ summary, activeRatingFilter, onSelectRating }) {
  if (!summary) {
    return null
  }

  const total = summary.totalReviews ?? 0
  const avg = Number(summary.averageRating ?? 0).toFixed(1)
  const stars = [5, 4, 3, 2, 1]

  const countByStar = {
    5: summary.star5Count ?? 0,
    4: summary.star4Count ?? 0,
    3: summary.star3Count ?? 0,
    2: summary.star2Count ?? 0,
    1: summary.star1Count ?? 0,
  }

  const percentByStar = summary.starPercentages ?? {
    5: total > 0 ? (countByStar[5] / total) * 100 : 0,
    4: total > 0 ? (countByStar[4] / total) * 100 : 0,
    3: total > 0 ? (countByStar[3] / total) * 100 : 0,
    2: total > 0 ? (countByStar[2] / total) * 100 : 0,
    1: total > 0 ? (countByStar[1] / total) * 100 : 0,
  }

  return (
    <div className="course-rating-summary">
      <div className="rating-summary__score">
        <div className="rating-summary__big-num">{total > 0 ? avg : '0.0'}</div>
        <div className="rating-summary__stars" aria-label={`Điểm đánh giá ${avg} trên 5`}>
          {[1, 2, 3, 4, 5].map((s) => {
            const fillRatio = Math.max(0, Math.min(1, Number(avg) - (s - 1)))
            return (
              <span
                key={s}
                className={`rating-star${fillRatio >= 0.75 ? ' rating-star--full' : fillRatio >= 0.25 ? ' rating-star--half' : ' rating-star--empty'}`}
                aria-hidden="true"
              >
                ★
              </span>
            )
          })}
        </div>
        <p className="rating-summary__count">
          {total > 0 ? `${total} lượt đánh giá` : 'Chưa có đánh giá nào'}
        </p>
      </div>

      <div className="rating-summary__bars">
        <div className="rating-summary__filter-head">
          <span className="muted small">Lọc theo số sao:</span>
          {activeRatingFilter != null && (
            <button
              type="button"
              className="tag tag--button"
              onClick={() => onSelectRating(null)}
              title="Xem tất cả đánh giá"
            >
              ✕ Bỏ lọc ({activeRatingFilter} sao)
            </button>
          )}
        </div>

        {stars.map((star) => {
          const count = countByStar[star]
          const percent = percentByStar[star] ?? 0
          const isActive = activeRatingFilter === star

          return (
            <button
              key={star}
              type="button"
              className={`rating-bar-row${isActive ? ' is-active' : ''}`}
              onClick={() => onSelectRating(isActive ? null : star)}
              title={`Xem ${count} đánh giá ${star} sao`}
              disabled={count === 0 && !isActive}
            >
              <span className="rating-bar-row__label">{star} sao</span>
              <div className="rating-bar-row__track">
                <div
                  className="rating-bar-row__fill"
                  style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
                />
              </div>
              <span className="rating-bar-row__percent muted small">
                {percent.toFixed(0)}% ({count})
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
