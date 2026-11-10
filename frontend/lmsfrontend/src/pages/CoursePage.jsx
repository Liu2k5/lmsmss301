import { useCallback, useEffect, useState } from 'react'
import { categoryApi, courseApi } from '../api/courseApi.js'
import { enrollmentApi } from '../api/enrollmentApi.js'
import ConfirmDialog from '../components/course/ConfirmDialog.jsx'
import CourseDetail from '../components/course/CourseDetail.jsx'
import FormModal from '../components/course/FormModal.jsx'
import { courseFields } from '../components/course/formFields.js'
import StatusBadge from '../components/course/StatusBadge.jsx'
import UploadModal from '../components/course/UploadModal.jsx'
import { STATUS_OPTIONS, formatDate } from '../components/course/courseMeta.js'
import { useAuth } from '../auth/AuthContext.js'

export default function CoursePage({ courseId, onOpenCourse, onBack }) {
  // Chỉ giảng viên được tạo & sửa nội dung khoá học. Admin chỉ quản lý hệ thống (danh mục, người dùng);
  // học viên / khách chỉ xem.
  const { authenticated, isInstructor, isAdmin } = useAuth()
  const canManage = authenticated && isInstructor
  // Học viên: được đăng ký / huỷ đăng ký khoá học. Admin chỉ quản lý hệ thống nên không đăng ký học.
  const isLearner = authenticated && !isInstructor && !isAdmin

  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)
  const [keyword, setKeyword] = useState('')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [categories, setCategories] = useState([])
  const [dialog, setDialog] = useState(null)
  const [toast, setToast] = useState(null)
  // Học viên: id các khoá học đang đăng ký (UC-E01) để hiện nút huỷ (UC-E02) ngay trên thẻ.
  const [enrolledIds, setEnrolledIds] = useState(() => new Set())
  const [cancellingId, setCancellingId] = useState(null)

  const loadCourses = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      // Lọc trạng thái chỉ dành cho giảng viên; danh sách công khai luôn là khoá đã xuất bản.
      const filters = { keyword: appliedKeyword, status: canManage ? status : '', categoryId }
      // Giảng viên: chỉ khoá của mình (gồm bản nháp). Khách / học viên: khoá công khai.
      setCourses(await (canManage ? courseApi.listMine(filters) : courseApi.list(filters)))
    } catch (error) {
      setListError(error)
    } finally {
      setLoading(false)
    }
  }, [appliedKeyword, status, categoryId, canManage])

  useEffect(() => {
    if (courseId == null) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- tải dữ liệu khi bộ lọc / màn hình thay đổi
      loadCourses()
    }
  }, [courseId, loadCourses])

  // Học viên: biết khoá nào đã đăng ký để hiện nút "Huỷ đăng ký" trên thẻ khoá học.
  useEffect(() => {
    if (courseId != null || !isLearner) return undefined
    let active = true
    enrollmentApi
      .myEnrollments()
      .then((list) => {
        if (!active) return
        setEnrolledIds(new Set(list.filter((item) => item.status === 'ACTIVE').map((item) => item.courseId)))
      })
      .catch(() => {
        if (active) setEnrolledIds(new Set())
      })
    return () => {
      active = false
    }
  }, [courseId, isLearner])

  // Danh mục cho ô lọc và form tạo khoá học (đọc công khai). Lỗi thì để trống, không chặn trang.
  useEffect(() => {
    let active = true
    categoryApi
      .list()
      .then((list) => active && setCategories(list))
      .catch(() => active && setCategories([]))
    return () => {
      active = false
    }
  }, [])

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
      fields: courseFields(categories),
      initialValues: {},
      submitLabel: 'Tạo khoá học',
      onSubmit: async ({ thumbnail, ...info }) => {
        // Khoá học luôn đứng tên giảng viên đang đăng nhập (sub trong JWT)
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

  /** UC-E02 — huỷ đăng ký khoá học ngay từ danh sách (Progress Service lưu trữ tiến độ). */
  function cancelEnrollment(course) {
    setDialog({
      kind: 'confirm',
      title: 'Huỷ đăng ký khoá học?',
      message: `Bạn sẽ mất quyền truy cập nội dung khoá học "${course.title}". Tiến độ hiện tại được lưu trữ lại, và có thể đăng ký học lại bất cứ lúc nào.`,
      confirmLabel: 'Huỷ đăng ký',
      danger: true,
      onConfirm: async () => {
        setDialog(null)
        setCancellingId(course.id)
        try {
          await enrollmentApi.cancel(course.id)
          setEnrolledIds((current) => {
            const next = new Set(current)
            next.delete(course.id)
            return next
          })
          notify('success', `Đã huỷ đăng ký khoá học "${course.title}".`)
        } catch (error) {
          // 404 = chưa từng đăng ký (ví dụ huỷ ở tab khác); coi như đã huỷ.
          if (error.status === 404) {
            setEnrolledIds((current) => {
              const next = new Set(current)
              next.delete(course.id)
              return next
            })
            notify('success', 'Bạn chưa đăng ký khoá học này.')
          } else {
            notify('error', error.message)
          }
        } finally {
          setCancellingId(null)
        }
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

  // Học viên và khách chỉ thấy khoá đang xuất bản (backend đã lọc; lọc lại phòng dữ liệu cũ).
  const visibleCourses = canManage ? courses : courses.filter((course) => course.status === 'PUBLISHED')

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
                <h1>{canManage ? 'Khoá học của tôi' : 'Khoá học'}</h1>
                <p>LearnHub LMS · Course Service</p>
              </div>
            </div>
            <div className="app-header__actions">
              <button type="button" className="button button--ghost" onClick={loadCourses} disabled={loading}>
                ⟳ Làm mới
              </button>
              {canManage && (
                <button type="button" className="button button--primary" onClick={openCreate}>
                  + Tạo khoá học
                </button>
              )}
            </div>
          </header>

          <section className={`stats${canManage ? ' stats--four' : ''}`} aria-label="Thống kê">
            <div className="stat">
              <span className="stat__value">{visibleCourses.length}</span>
              <span className="stat__label">Khoá học{status || appliedKeyword || categoryId ? ' (đang lọc)' : ''}</span>
            </div>
            <div className="stat">
              <span className="stat__value">{counts.PUBLISHED ?? 0}</span>
              <span className="stat__label">Đã xuất bản</span>
            </div>
            <div className="stat">
              <span className="stat__value">{counts.UNPUBLISHED ?? 0}</span>
              <span className="stat__label">Đã ẩn</span>
            </div>
            {canManage && (
              <div className="stat">
                <span className="stat__value">{counts.DRAFT ?? 0}</span>
                <span className="stat__label">Bản nháp</span>
              </div>
            )}
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
            <select aria-label="Lọc theo danh mục" value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            {canManage && (
              <select aria-label="Lọc theo trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}>
                <option value="">Tất cả trạng thái</option>
                {STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            )}
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
          ) : visibleCourses.length === 0 ? (
            !listError && (
              <div className="state-card">
                <div className="state-card__icon" aria-hidden="true">
                  📚
                </div>
                <h2>{status || appliedKeyword || categoryId ? 'Không có khoá học phù hợp' : 'Chưa có khoá học nào'}</h2>
                <p>
                  {status || appliedKeyword || categoryId
                    ? 'Thử đổi từ khoá hoặc bộ lọc trạng thái.'
                    : canManage
                      ? 'Tạo khoá học đầu tiên rồi thêm chương, bài học và học liệu.'
                      : 'Hiện chưa có khoá học nào được xuất bản để tham gia.'}
                </p>
                {canManage && !status && !appliedKeyword && !categoryId && (
                  <button type="button" className="button button--primary" onClick={openCreate}>
                    + Tạo khoá học đầu tiên
                  </button>
                )}
              </div>
            )
          ) : (
            <div className="course-grid">
              {visibleCourses.map((course) => (
                <div className="course-card-item" key={course.id}>
                <button
                  type="button"
                  className="course-card"
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
                      <span className="course-card__id">#{course.id.slice(0, 8)}</span>
                    </div>
                    <h2 className="course-card__title">{course.title}</h2>
                    {course.description && <p className="course-card__desc">{course.description}</p>}
                    <dl className="course-card__meta">
                      <div>
                        <dt>Danh mục</dt>
                        <dd>{course.categoryName || 'Chưa có'}</dd>
                      </div>
                      <div>
                        <dt>Cập nhật</dt>
                        <dd>{formatDate(course.updatedAt)}</dd>
                      </div>
                    </dl>
                  </div>
                </button>
                {isLearner && enrolledIds.has(course.id) && (
                  <div className="course-card-item__actions">
                    <span className="badge badge--joined">✓ Đã tham gia</span>
                    <button
                      type="button"
                      className="button button--danger-ghost button--small"
                      onClick={() => cancelEnrollment(course)}
                      disabled={cancellingId === course.id}
                    >
                      {cancellingId === course.id ? 'Đang huỷ…' : 'Huỷ đăng ký'}
                    </button>
                  </div>
                )}
                </div>
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
          title={dialog.title}
          subtitle={dialog.subtitle}
          defaultType={dialog.defaultType}
          initialName={dialog.initialName}
          submitLabel={dialog.submitLabel}
          notice={dialog.notice}
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
