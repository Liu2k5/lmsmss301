import { useCallback, useEffect, useState } from 'react'
import { courseApi } from '../api/courseApi.js'
import ConfirmDialog from '../components/course/ConfirmDialog.jsx'
import CourseDetail from '../components/course/CourseDetail.jsx'
import FormModal from '../components/course/FormModal.jsx'
import { COURSE_FIELDS } from '../components/course/formFields.js'
import StatusBadge from '../components/course/StatusBadge.jsx'
import UploadModal from '../components/course/UploadModal.jsx'
import { STATUS_OPTIONS, formatDate } from '../components/course/courseMeta.js'

export default function CoursePage({ courseId, onOpenCourse, onBack }) {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)
  const [keyword, setKeyword] = useState('')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [dialog, setDialog] = useState(null)
  const [toast, setToast] = useState(null)

  const loadCourses = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      setCourses(await courseApi.list({ keyword: appliedKeyword, status }))
    } catch (error) {
      setListError(error)
    } finally {
      setLoading(false)
    }
  }, [appliedKeyword, status])

  useEffect(() => {
    if (courseId == null) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- tải dữ liệu khi bộ lọc / màn hình thay đổi
      loadCourses()
    }
  }, [courseId, loadCourses])

  // Tìm kiếm khi người dùng ngừng gõ 350ms.
  useEffect(() => {
    const timer = setTimeout(() => setAppliedKeyword(keyword.trim()), 350)
    return () => clearTimeout(timer)
  }, [keyword])

  useEffect(() => {
    if (!toast) return undefined
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  const notify = useCallback((type, message) => setToast({ type, message }), [])
  const closeDialog = useCallback(() => setDialog(null), [])

  function openCreate() {
    setDialog({
      kind: 'form',
      title: 'Tạo khoá học mới',
      subtitle: 'Khoá học mới luôn ở trạng thái Bản nháp cho tới khi bạn xuất bản.',
      fields: COURSE_FIELDS,
      initialValues: { instructorId: 1 },
      submitLabel: 'Tạo khoá học',
      onSubmit: async ({ thumbnail, ...info }) => {
        const created = await courseApi.create(info)
        setDialog(null)
        // Khoá học đã tạo; ảnh bìa upload sau vì cần id khoá học. Lỗi ảnh không làm mất khoá học.
        if (thumbnail?.file) {
          try {
            await courseApi.uploadThumbnail(created.id, thumbnail.file)
          } catch (error) {
            notify('error', `Đã tạo khoá học nhưng chưa lưu được ảnh bìa: ${error.message}`)
            onOpenCourse(created.id)
            return
          }
        }
        notify('success', `Đã tạo khoá học "${created.title}".`)
        onOpenCourse(created.id)
      },
    })
  }

  const counts = courses.reduce(
    (acc, course) => {
      acc[course.status] = (acc[course.status] ?? 0) + 1
      return acc
    },
    {},
  )

  return (
    <div className="app">
      {courseId != null ? (
        <CourseDetail
          key={courseId}
          courseId={courseId}
          onBack={onBack}
          openDialog={setDialog}
          closeDialog={closeDialog}
          notify={notify}
        />
      ) : (
        <>
          <header className="app-header">
            <div className="app-header__brand">
              <div className="app-header__logo" aria-hidden="true">
                C
              </div>
              <div>
                <h1>Quản lý khoá học</h1>
                <p>LearnHub LMS · Course Service</p>
              </div>
            </div>
            <div className="app-header__actions">
              <button type="button" className="button button--ghost" onClick={loadCourses} disabled={loading}>
                ⟳ Làm mới
              </button>
              <button type="button" className="button button--primary" onClick={openCreate}>
                + Tạo khoá học
              </button>
            </div>
          </header>

          <section className="stats stats--four" aria-label="Thống kê">
            <div className="stat">
              <span className="stat__value">{courses.length}</span>
              <span className="stat__label">Khoá học{status || appliedKeyword ? ' (đang lọc)' : ''}</span>
            </div>
            <div className="stat">
              <span className="stat__value">{counts.PUBLISHED ?? 0}</span>
              <span className="stat__label">Đã xuất bản</span>
            </div>
            <div className="stat">
              <span className="stat__value">{counts.UNPUBLISHED ?? 0}</span>
              <span className="stat__label">Đã ẩn</span>
            </div>
            <div className="stat">
              <span className="stat__value">{counts.DRAFT ?? 0}</span>
              <span className="stat__label">Bản nháp</span>
            </div>
          </section>

          <div className="toolbar" role="search">
            <input
              type="text"
              className="toolbar__search"
              placeholder="Tìm theo tên hoặc mô tả khoá học…"
              aria-label="Tìm khoá học"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <select aria-label="Lọc theo trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">Tất cả trạng thái</option>
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          {listError && (
            <div className="alert alert--error" role="alert">
              <strong>{listError.message}</strong>
              <button type="button" className="button button--ghost button--small" onClick={loadCourses}>
                Thử lại
              </button>
            </div>
          )}

          {loading ? (
            <div className="state-card">
              <span className="spinner" aria-hidden="true" />
              <p>Đang tải danh sách khoá học…</p>
            </div>
          ) : courses.length === 0 ? (
            !listError && (
              <div className="state-card">
                <div className="state-card__icon" aria-hidden="true">
                  📚
                </div>
                <h2>{status || appliedKeyword ? 'Không có khoá học phù hợp' : 'Chưa có khoá học nào'}</h2>
                <p>
                  {status || appliedKeyword
                    ? 'Thử đổi từ khoá hoặc bộ lọc trạng thái.'
                    : 'Tạo khoá học đầu tiên rồi thêm chương, bài học và học liệu.'}
                </p>
                {!status && !appliedKeyword && (
                  <button type="button" className="button button--primary" onClick={openCreate}>
                    + Tạo khoá học đầu tiên
                  </button>
                )}
              </div>
            )
          ) : (
            <div className="course-grid">
              {courses.map((course) => (
                <button
                  type="button"
                  className="course-card"
                  key={course.id}
                  onClick={() => onOpenCourse(course.id)}
                >
                  <div className="course-card__thumb">
                    {course.thumbnailUrl ? (
                      <img src={course.thumbnailUrl} alt="" loading="lazy" />
                    ) : (
                      <span aria-hidden="true">{course.title.slice(0, 1).toUpperCase()}</span>
                    )}
                  </div>
                  <div className="course-card__body">
                    <div className="course-card__top">
                      <StatusBadge status={course.status} />
                      <span className="course-card__id">#{course.id}</span>
                    </div>
                    <h2 className="course-card__title">{course.title}</h2>
                    {course.description && <p className="course-card__desc">{course.description}</p>}
                    <dl className="course-card__meta">
                      <div>
                        <dt>Chương</dt>
                        <dd>{course.sectionCount}</dd>
                      </div>
                      <div>
                        <dt>Bài học</dt>
                        <dd>{course.lessonCount}</dd>
                      </div>
                      <div>
                        <dt>Cập nhật</dt>
                        <dd>{formatDate(course.updatedAt)}</dd>
                      </div>
                    </dl>
                  </div>
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {dialog?.kind === 'form' && (
        <FormModal
          title={dialog.title}
          subtitle={dialog.subtitle}
          fields={dialog.fields}
          initialValues={dialog.initialValues}
          submitLabel={dialog.submitLabel}
          onSubmit={dialog.onSubmit}
          onCancel={closeDialog}
        />
      )}

      {dialog?.kind === 'upload' && (
        <UploadModal
          subtitle={dialog.subtitle}
          defaultType={dialog.defaultType}
          onUpload={dialog.onUpload}
          onCancel={closeDialog}
        />
      )}

      {dialog?.kind === 'confirm' && (
        <ConfirmDialog
          title={dialog.title}
          message={dialog.message}
          confirmLabel={dialog.confirmLabel}
          danger={dialog.danger ?? true}
          onConfirm={dialog.onConfirm}
          onCancel={closeDialog}
        />
      )}

      {toast && (
        <div className={`toast toast--${toast.type}`} role="status">
          {toast.message}
        </div>
      )}
    </div>
  )
}
