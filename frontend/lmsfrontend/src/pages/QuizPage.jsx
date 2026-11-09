import { useCallback, useEffect, useRef, useState } from 'react'
import QuizForm from '../components/QuizForm.jsx'
import QuizPreview from '../components/QuizPreview.jsx'
import { quizApi } from '../api/quizApi.js'
import { useAuth } from '../auth/AuthContext.js'

function formatDate(value) {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Trang quản lý quiz của giảng viên.
 *
 * Hỗ trợ deep-link từ trang khoá học:
 * - `#/quizzes?quizId=5`  → mở ngay form sửa quiz 5
 * - `#/quizzes?lessonId=7` → mở form tạo quiz cho bài học 7
 */
export default function QuizPage({ initialLessonId, initialQuizId }) {
  const [quizzes, setQuizzes] = useState([])
  const [lessonOptions, setLessonOptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)

  const [filters, setFilters] = useState({ keyword: '', courseId: '' })
  const [activeKeyword, setActiveKeyword] = useState('')

  const [editor, setEditor] = useState(null)
  const [openingEditor, setOpeningEditor] = useState(false)
  const [previewId, setPreviewId] = useState(null)
  const [duplicateTarget, setDuplicateTarget] = useState(null)
  const [duplicateLessonId, setDuplicateLessonId] = useState('')
  const [duplicating, setDuplicating] = useState(false)
  const [confirmTarget, setConfirmTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [toast, setToast] = useState(null)
  const handledLink = useRef(null)

  // Quiz là công cụ soạn bài: chỉ giảng viên (hoặc admin) được dùng.
  const { ready, authenticated, isInstructor, isAdmin } = useAuth()
  const canManage = authenticated && (isInstructor || isAdmin)

  useEffect(() => {
    const timer = setTimeout(() => setActiveKeyword(filters.keyword.trim()), 300)
    return () => clearTimeout(timer)
  }, [filters.keyword])

  const loadQuizzes = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      setQuizzes(await quizApi.list({ keyword: activeKeyword, courseId: filters.courseId }))
    } catch (error) {
      setListError(error)
    } finally {
      setLoading(false)
    }
  }, [activeKeyword, filters.courseId])

  const loadLessonOptions = useCallback(async () => {
    try {
      setLessonOptions(await quizApi.lessonOptions())
    } catch (error) {
      setToast({ type: 'error', message: error.message })
    }
  }, [])

  // Tải danh sách khi bộ lọc thay đổi (state chỉ set trong callback bất đồng bộ).
  useEffect(() => {
    if (!authenticated) {
      return undefined
    }
    let active = true
    quizApi
      .list({ keyword: activeKeyword, courseId: filters.courseId })
      .then((data) => {
        if (active) {
          setQuizzes(data)
          setListError(null)
        }
      })
      .catch((error) => {
        if (active) {
          setListError(error)
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [activeKeyword, filters.courseId, authenticated])

  // Tải danh sách bài học loại QUIZ (dùng cho dropdown + thống kê).
  useEffect(() => {
    if (!canManage) {
      return undefined
    }
    let active = true
    quizApi
      .lessonOptions()
      .then((lessons) => active && setLessonOptions(lessons))
      .catch((error) => active && setToast({ type: 'error', message: error.message }))
    return () => {
      active = false
    }
  }, [canManage])

  // Deep-link: #/quizzes?quizId=… hoặc #/quizzes?lessonId=… (hash đổi cũng kích hoạt lại).
  useEffect(() => {
    const link = initialQuizId ? `q:${initialQuizId}` : initialLessonId ? `l:${initialLessonId}` : null
    if (!canManage || !link || handledLink.current === link) {
      return undefined
    }

    let active = true
    const done = () => {
      handledLink.current = link
    }

    const task = initialQuizId
      ? quizApi.get(initialQuizId).then((quiz) => {
          if (active) {
            done()
            setEditor({ mode: 'edit', quiz })
          }
        })
      : quizApi.lessonOptions().then((lessons) => {
          if (!active) {
            return
          }
          setLessonOptions(lessons)
          const lesson = lessons.find((item) => item.id === initialLessonId)
          if (!lesson) {
            done()
            return
          }
          if (lesson.quizId) {
            quizApi
              .get(lesson.quizId)
              .then((quiz) => {
                if (active) {
                  done()
                  setEditor({ mode: 'edit', quiz })
                }
              })
              .catch((error) => active && setToast({ type: 'error', message: error.message }))
          } else {
            done()
            setEditor({ mode: 'create', lessonId: lesson.id })
          }
        })

    task.catch((error) => active && setToast({ type: 'error', message: error.message }))
    return () => {
      active = false
    }
  }, [initialLessonId, initialQuizId, canManage])

  function applyFilters(patch) {
    setLoading(true)
    setFilters((current) => ({ ...current, ...patch }))
  }

  useEffect(() => {
    if (!toast) {
      return undefined
    }
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  async function refresh() {
    await Promise.all([loadQuizzes(), loadLessonOptions()])
  }

  async function openEdit(quiz) {
    setOpeningEditor(true)
    try {
      setEditor({ mode: 'edit', quiz: await quizApi.get(quiz.id) })
    } catch (error) {
      setToast({ type: 'error', message: error.message })
    } finally {
      setOpeningEditor(false)
    }
  }

  async function saveQuiz(payload) {
    if (editor?.mode === 'edit') {
      await quizApi.update(editor.quiz.id, payload)
      setToast({ type: 'success', message: 'Đã cập nhật quiz.' })
    } else {
      await quizApi.create(payload)
      setToast({ type: 'success', message: 'Đã tạo quiz mới.' })
    }
    setEditor(null)
    await refresh()
  }

  async function duplicateQuiz() {
    if (!duplicateLessonId) {
      return
    }
    setDuplicating(true)
    try {
      const copy = await quizApi.duplicate(duplicateTarget.id, duplicateLessonId)
      setToast({ type: 'success', message: `Đã nhân bản sang "${copy.lessonTitle}".` })
      setDuplicateTarget(null)
      setDuplicateLessonId('')
      await refresh()
    } catch (error) {
      setToast({ type: 'error', message: error.message })
    } finally {
      setDuplicating(false)
    }
  }

  async function deleteQuiz() {
    setDeleting(true)
    try {
      await quizApi.remove(confirmTarget.id)
      setToast({ type: 'success', message: `Đã xoá quiz "${confirmTarget.title}".` })
      setConfirmTarget(null)
      await refresh()
    } catch (error) {
      setToast({ type: 'error', message: error.message })
    } finally {
      setDeleting(false)
    }
  }

  if (!ready) {
    return (
      <div className="app app--message">
        <p>Đang kiểm tra quyền truy cập…</p>
      </div>
    )
  }

  if (!authenticated) {
    return (
      <div className="app app--message">
        <h1>Bài quiz</h1>
        <p>Vui lòng đăng nhập để làm bài hoặc soạn quiz (giảng viên).</p>
        <a className="button button--primary" href="#/login">
          Đăng nhập
        </a>
      </div>
    )
  }

  const courseSource = canManage
    ? lessonOptions.map((lesson) => [lesson.courseId, lesson.courseTitle])
    : quizzes.map((quiz) => [quiz.courseId, quiz.courseTitle])
  const courses = Array.from(new Map(courseSource).entries()).map(([id, title]) => ({ id, title }))

  const emptyLessons = lessonOptions.filter((lesson) => lesson.quizId == null)
  const totalQuestions = quizzes.reduce((sum, quiz) => sum + quiz.questionCount, 0)
  const filtering = Boolean(activeKeyword || filters.courseId)

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__brand">
          <div className="app-header__logo" aria-hidden="true">
            Q
          </div>
          <div>
            <h1>{canManage ? 'Quản lý bài Quiz' : 'Bài quiz'}</h1>
            <p>LearnHub LMS · Course Service</p>
          </div>
        </div>
        <div className="app-header__actions">
          <button type="button" className="button button--ghost" onClick={refresh} disabled={loading}>
            ⟳ Làm mới
          </button>
          {canManage && (
            <button
              type="button"
              className="button button--primary"
              onClick={() => setEditor({ mode: 'create' })}
              disabled={openingEditor || loading}
            >
              + Tạo quiz
            </button>
          )}
        </div>
      </header>

      <section className="stats" aria-label="Thống kê">
        <div className="stat">
          <span className="stat__value">{quizzes.length}</span>
          <span className="stat__label">{filtering ? 'Quiz khớp bộ lọc' : 'Bài quiz'}</span>
        </div>
        <div className="stat">
          <span className="stat__value">{totalQuestions}</span>
          <span className="stat__label">Câu hỏi</span>
        </div>
        {canManage && (
          <div className="stat">
            <span className="stat__value">{emptyLessons.length}</span>
            <span className="stat__label">Bài học còn trống</span>
          </div>
        )}
      </section>

      <section className="toolbar" aria-label="Bộ lọc">
        <label className="toolbar__search">
          <span className="sr-only">Tìm kiếm quiz</span>
          <input
            type="search"
            placeholder="Tìm theo tên quiz, bài học hoặc khoá học…"
            value={filters.keyword}
            onChange={(event) => applyFilters({ keyword: event.target.value })}
          />
        </label>
        <label className="toolbar__select">
          <span className="sr-only">Lọc theo khoá học</span>
          <select
            value={filters.courseId}
            onChange={(event) => applyFilters({ courseId: event.target.value })}
          >
            <option value="">Tất cả khoá học</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.title}
              </option>
            ))}
          </select>
        </label>
        {filtering && (
          <button
            type="button"
            className="button button--ghost button--small"
            onClick={() => setFilters({ keyword: '', courseId: '' })}
          >
            Xoá bộ lọc
          </button>
        )}
      </section>

      {listError && (
        <div className="alert alert--error" role="alert">
          <strong>{listError.message}</strong>
          <button type="button" className="button button--ghost button--small" onClick={loadQuizzes}>
            Thử lại
          </button>
        </div>
      )}

      {loading ? (
        <div className="state-card">
          <span className="spinner" aria-hidden="true" />
          <p>Đang tải danh sách quiz…</p>
        </div>
      ) : quizzes.length === 0 ? (
        <div className="state-card">
          <div className="state-card__icon" aria-hidden="true">
            🗒️
          </div>
          <h2>{filtering ? 'Không có quiz nào khớp bộ lọc' : 'Chưa có bài quiz nào'}</h2>
          <p>
            {filtering
              ? 'Thử từ khoá khác hoặc bỏ bộ lọc.'
              : 'Tạo quiz đầu tiên cho một bài học loại QUIZ để bắt đầu.'}
          </p>
          {filtering ? (
            <button
              type="button"
              className="button button--ghost"
              onClick={() => setFilters({ keyword: '', courseId: '' })}
            >
              Xoá bộ lọc
            </button>
          ) : (
            <button type="button" className="button button--primary" onClick={() => setEditor({ mode: 'create' })}>
              + Tạo quiz đầu tiên
            </button>
          )}
        </div>
      ) : (
        <div className="quiz-grid">
          {quizzes.map((quiz) => (
            <article className="quiz-card" key={quiz.id}>
              <header className="quiz-card__header">
                <span className="quiz-card__score">
                  {Number(quiz.passingScore)}% để đạt
                  {quiz.maxAttempts ? ` · tối đa ${quiz.maxAttempts} lần` : ' · không giới hạn lượt'}
                </span>
              </header>
              <h2 className="quiz-card__title">{quiz.title}</h2>
              <p className="quiz-card__lesson">{quiz.lessonTitle}</p>
              <p className="quiz-card__course">
                {quiz.courseTitle}
                {quiz.sectionTitle ? ` · ${quiz.sectionTitle}` : ''}
              </p>

              <dl className="quiz-card__meta">
                <div>
                  <dt>Số câu hỏi</dt>
                  <dd>{quiz.questionCount}</dd>
                </div>
                <div>
                  <dt>Cập nhật</dt>
                  <dd>{formatDate(quiz.updatedAt)}</dd>
                </div>
              </dl>

              <footer className="quiz-card__actions">
                <a
                  className="button button--primary"
                  href={`#/quiz-attempt?quizId=${quiz.id}&courseId=${quiz.courseId}`}
                >
                  Làm bài
                </a>
                {canManage && (
                <button
                  type="button"
                  className="button button--ghost"
                  onClick={() => openEdit(quiz)}
                  disabled={openingEditor}
                >
                  Sửa
                </button>
                )}
                <button type="button" className="button button--ghost" onClick={() => setPreviewId(quiz.id)}>
                  Xem trước
                </button>
                {canManage && (
                  <>
                    <button
                      type="button"
                      className="button button--ghost"
                      onClick={() => {
                        setDuplicateTarget(quiz)
                        setDuplicateLessonId('')
                      }}
                    >
                      Nhân bản
                    </button>
                    <button
                      type="button"
                      className="button button--danger-ghost"
                      onClick={() => setConfirmTarget(quiz)}
                    >
                      Xoá
                    </button>
                  </>
                )}
              </footer>
            </article>
          ))}
        </div>
      )}

      {editor && (
        <QuizForm
          key={editor.mode === 'edit' ? `edit-${editor.quiz.id}` : `create-${editor.lessonId ?? 'any'}`}
          quiz={editor.mode === 'edit' ? editor.quiz : null}
          initialLessonId={editor.mode === 'create' ? editor.lessonId : null}
          lessonOptions={lessonOptions}
          onSubmit={saveQuiz}
          onCancel={() => setEditor(null)}
        />
      )}

      {previewId && <QuizPreview quizId={previewId} onClose={() => setPreviewId(null)} />}

      {duplicateTarget && (
        <div className="modal-backdrop" role="presentation">
          <div className="modal modal--small" role="dialog" aria-modal="true" aria-labelledby="duplicate-title">
            <header className="modal__header">
              <div>
                <h2 id="duplicate-title">Nhân bản quiz</h2>
                <p className="modal__subtitle">
                  Sao chép toàn bộ câu hỏi và đáp án của “{duplicateTarget.title}”.
                </p>
              </div>
              <button
                type="button"
                className="icon-button"
                onClick={() => setDuplicateTarget(null)}
                aria-label="Đóng"
                disabled={duplicating}
              >
                ✕
              </button>
            </header>
            <div className="modal__body">
              <label className="field">
                <span className="field__label">Bài học đích (chưa có quiz)</span>
                <select value={duplicateLessonId} onChange={(event) => setDuplicateLessonId(event.target.value)}>
                  <option value="">— Chọn bài học —</option>
                  {emptyLessons.map((lesson) => (
                    <option key={lesson.id} value={lesson.id}>
                      {lesson.courseTitle} › {lesson.sectionTitle} › {lesson.title}
                    </option>
                  ))}
                </select>
                {emptyLessons.length === 0 && (
                  <span className="field__hint">Không còn bài học QUIZ nào trống để nhân bản tới.</span>
                )}
              </label>
            </div>
            <footer className="modal__footer">
              <button
                type="button"
                className="button button--ghost"
                onClick={() => setDuplicateTarget(null)}
                disabled={duplicating}
              >
                Huỷ
              </button>
              <button
                type="button"
                className="button button--primary"
                onClick={duplicateQuiz}
                disabled={!duplicateLessonId || duplicating}
              >
                {duplicating ? 'Đang nhân bản…' : 'Nhân bản'}
              </button>
            </footer>
          </div>
        </div>
      )}

      {confirmTarget && (
        <div className="modal-backdrop" role="presentation">
          <div className="modal modal--small" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
            <header className="modal__header">
              <h2 id="confirm-title">Xoá bài quiz?</h2>
            </header>
            <div className="modal__body">
              <p>
                Quiz <strong>{confirmTarget.title}</strong> và toàn bộ câu hỏi, đáp án của nó sẽ bị xoá. Thao tác này
                không thể hoàn tác.
              </p>
            </div>
            <footer className="modal__footer">
              <button
                type="button"
                className="button button--ghost"
                onClick={() => setConfirmTarget(null)}
                disabled={deleting}
              >
                Huỷ
              </button>
              <button type="button" className="button button--danger" onClick={deleteQuiz} disabled={deleting}>
                {deleting ? 'Đang xoá…' : 'Xoá quiz'}
              </button>
            </footer>
          </div>
        </div>
      )}

      {toast && (
        <div className={`toast toast--${toast.type}`} role="status">
          {toast.message}
        </div>
      )}
    </div>
  )
}
