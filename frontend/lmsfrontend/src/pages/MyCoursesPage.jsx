import { useCallback, useEffect, useState } from 'react'
import { enrollmentApi } from '../api/enrollmentApi.js'
import { courseApi } from '../api/courseApi.js'
import { useAuth } from '../auth/AuthContext.js'
import ConfirmDialog from '../components/course/ConfirmDialog.jsx'
import { formatDate } from '../components/course/courseMeta.js'

export default function MyCoursesPage() {
  const { authenticated, isStudent, isAdmin } = useAuth()
  const [enrollments, setEnrollments] = useState([])
  const [coursesMap, setCoursesMap] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [cancelTarget, setCancelTarget] = useState(null)
  const [toast, setToast] = useState(null)
  const [actionBusy, setActionBusy] = useState(false)

  const loadData = useCallback(async () => {
    if (!authenticated || !isStudent) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const [enrolledList, allCourses] = await Promise.all([
        enrollmentApi.myEnrollments(),
        courseApi.list().catch(() => []),
      ])
      setEnrollments(enrolledList)
      const map = {}
      for (const c of allCourses) {
        map[c.id] = c
      }
      setCoursesMap(map)
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách khoá học đã đăng ký.')
    } finally {
      setLoading(false)
    }
  }, [authenticated, isStudent])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    if (!toast) return undefined
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  async function handleCancelEnrollment(courseId) {
    setActionBusy(true)
    try {
      await enrollmentApi.cancel(courseId)
      setToast({ type: 'success', message: 'Đã huỷ đăng ký khoá học thành công. Tiến độ học của bạn đã được lưu trữ an toàn.' })
      setCancelTarget(null)
      await loadData()
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Không thể huỷ đăng ký khoá học.' })
    } finally {
      setActionBusy(false)
    }
  }

  async function handleReEnroll(courseId) {
    setActionBusy(true)
    try {
      await enrollmentApi.enroll(courseId)
      setToast({ type: 'success', message: 'Đã đăng ký lại khoá học thành công!' })
      await loadData()
    } catch (err) {
      setToast({ type: 'error', message: err.message || 'Không thể đăng ký lại khoá học.' })
    } finally {
      setActionBusy(false)
    }
  }

  if (!authenticated) {
    return (
      <main className="user-page">
        <div className="state-card">
          <h2>Khoá học của tôi</h2>
          <p>Vui lòng đăng nhập với tài khoản học viên để xem danh sách khoá học bạn đã đăng ký và theo dõi tiến độ.</p>
          <a href="#/login" className="button button--primary" style={{ marginTop: 16 }}>Đăng nhập ngay</a>
        </div>
      </main>
    )
  }

  if (!isStudent) {
    return (
      <main className="user-page">
        <div className="state-card">
          <h2>Khoá học của tôi</h2>
          <p>
            Chức năng này dành riêng cho tài khoản <strong>Học viên</strong>. Bạn đang đăng nhập với vai trò{' '}
            <strong>{isAdmin ? 'Quản trị viên' : 'Giảng viên'}</strong>.
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 16 }}>
            <a href="#/courses" className="button button--primary">
              Quản lý khoá học
            </a>
            {isAdmin && (
              <a href="#/admin/users" className="button button--ghost">
                Quản lý người dùng
              </a>
            )}
          </div>
        </div>
      </main>
    )
  }

  const filteredEnrollments = enrollments.filter((item) => {
    if (statusFilter === 'ALL') return true
    return item.status === statusFilter
  })

  return (
    <main className="app">
      {toast && (
        <div className={`alert alert--${toast.type === 'error' ? 'error' : 'success'}`} role="status">
          {toast.message}
        </div>
      )}

      {cancelTarget && (
        <ConfirmDialog
          title="Huỷ đăng ký khoá học?"
          message={`Bạn có chắc chắn muốn huỷ đăng ký khoá học "${coursesMap[cancelTarget]?.title || 'này'}"? Tiến độ học tập và các bài quiz của bạn sẽ được lưu trữ vào kho lưu trữ (archive).`}
          confirmLabel="Huỷ đăng ký"
          danger={true}
          onConfirm={() => handleCancelEnrollment(cancelTarget)}
          onCancel={() => setCancelTarget(null)}
        />
      )}

      <header className="page-header">
        <div>
          <span className="eyebrow">HỌC TẬP · TIẾN ĐỘ</span>
          <h1>Khoá học của tôi</h1>
          <p className="muted">Quản lý các khoá học bạn đã đăng ký và tiếp tục hành trình học tập.</p>
        </div>
        <a href="#/courses" className="button button--ghost">+ Khám phá thêm khoá học</a>
      </header>

      <div className="filter-bar" style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <button
          type="button"
          className={`button button--small ${statusFilter === 'ALL' ? 'button--primary' : 'button--ghost'}`}
          onClick={() => setStatusFilter('ALL')}
        >
          Tất cả ({enrollments.length})
        </button>
        <button
          type="button"
          className={`button button--small ${statusFilter === 'ACTIVE' ? 'button--primary' : 'button--ghost'}`}
          onClick={() => setStatusFilter('ACTIVE')}
        >
          Đang học ({enrollments.filter((e) => e.status === 'ACTIVE').length})
        </button>
        <button
          type="button"
          className={`button button--small ${statusFilter === 'CANCELLED' ? 'button--primary' : 'button--ghost'}`}
          onClick={() => setStatusFilter('CANCELLED')}
        >
          Đã huỷ ({enrollments.filter((e) => e.status === 'CANCELLED').length})
        </button>
      </div>

      {loading ? (
        <div className="state-card">
          <span className="spinner" aria-hidden="true" />
          <p>Đang tải danh sách khoá học của bạn…</p>
        </div>
      ) : error ? (
        <div className="alert alert--error" role="alert">
          <strong>Lỗi:</strong> {error}
          <button type="button" className="button button--ghost button--small" onClick={loadData} style={{ marginLeft: 12 }}>
            Thử lại
          </button>
        </div>
      ) : filteredEnrollments.length === 0 ? (
        <div className="state-card">
          <p>
            {statusFilter === 'ALL'
              ? 'Bạn chưa đăng ký khoá học nào. Hãy khám phá và chọn khoá học phù hợp!'
              : `Không có khoá học nào ở trạng thái "${statusFilter}".`}
          </p>
          <a href="#/courses" className="button button--primary" style={{ marginTop: 12 }}>
            Khám phá khoá học
          </a>
        </div>
      ) : (
        <div className="course-grid">
          {filteredEnrollments.map((enr) => {
            const course = coursesMap[enr.courseId]
            const title = course?.title || `Khoá học #${enr.courseId.slice(0, 8)}`
            const desc = course?.description || 'Khoá học trực tuyến trên LearnHub'
            const isCancelled = enr.status === 'CANCELLED'
            const isSuspended = enr.status === 'SUSPENDED'

            return (
              <article className="course-card" key={enr.id}>
                <a href={`#/courses/${enr.courseId}`} className="course-card__cover" tabIndex={-1} aria-hidden="true">
                  {course?.thumbnailUrl ? (
                    <img src={course.thumbnailUrl} alt={title} loading="lazy" />
                  ) : (
                    <span className="course-card__cover-placeholder">✦</span>
                  )}
                </a>
                <div className="course-card__body">
                  <div className="course-card__top">
                    <span
                      className={`status-badge status-badge--${
                        enr.status === 'ACTIVE' ? 'success' : isCancelled ? 'muted' : 'dark'
                      }`}
                    >
                      {enr.status === 'ACTIVE' ? 'Đang học' : isCancelled ? 'Đã huỷ' : 'Tạm khoá'}
                    </span>
                    <span className="muted small">Đăng ký: {formatDate(enr.enrolledAt)}</span>
                  </div>
                  <h3 className="course-card__title">
                    <a href={`#/courses/${enr.courseId}`}>{title}</a>
                  </h3>
                  <p className="course-card__desc">{desc}</p>
                  {course && (
                    <p className="muted small" style={{ marginBottom: 16 }}>
                      {course.sectionCount ?? 0} chương · {course.lessonCount ?? 0} bài học
                    </p>
                  )}
                  <div className="course-card__footer" style={{ display: 'flex', gap: 8, justifyContent: 'space-between', alignItems: 'center' }}>
                    <a href={`#/courses/${enr.courseId}`} className="button button--primary button--small">
                      {isCancelled ? 'Xem khoá học' : 'Vào học ngay →'}
                    </a>
                    {enr.status === 'ACTIVE' ? (
                      <button
                        type="button"
                        className="button button--ghost button--small text-danger"
                        onClick={() => setCancelTarget(enr.courseId)}
                        disabled={actionBusy}
                        title="Huỷ đăng ký khoá học này"
                      >
                        Huỷ đăng ký
                      </button>
                    ) : isCancelled ? (
                      <button
                        type="button"
                        className="button button--ghost button--small"
                        onClick={() => handleReEnroll(enr.courseId)}
                        disabled={actionBusy}
                      >
                        Đăng ký lại
                      </button>
                    ) : null}
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </main>
  )
}
