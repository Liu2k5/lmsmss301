import { useCallback, useEffect, useState } from 'react'
import './App.css'
import QuizForm from './components/QuizForm.jsx'
import { quizApi } from './api/quizApi.js'

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

export default function App() {
  const [quizzes, setQuizzes] = useState([])
  const [lessonOptions, setLessonOptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)
  const [editor, setEditor] = useState(null)
  const [openingEditor, setOpeningEditor] = useState(false)
  const [confirmTarget, setConfirmTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [toast, setToast] = useState(null)

  const fetchAll = useCallback(
    () => Promise.all([quizApi.list(), quizApi.lessonOptions()]),
    [],
  )

  const loadData = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      const [quizList, lessons] = await fetchAll()
      setQuizzes(quizList)
      setLessonOptions(lessons)
    } catch (error) {
      setListError(error)
    } finally {
      setLoading(false)
    }
  }, [fetchAll])

  useEffect(() => {
    let active = true
    fetchAll()
      .then(([quizList, lessons]) => {
        if (active) {
          setQuizzes(quizList)
          setLessonOptions(lessons)
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
  }, [fetchAll])

  useEffect(() => {
    if (!toast) {
      return undefined
    }
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  async function openEdit(quiz) {
    setOpeningEditor(true)
    try {
      const detail = await quizApi.get(quiz.id)
      setEditor({ mode: 'edit', quiz: detail })
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
    await loadData()
  }

  async function deleteQuiz() {
    setDeleting(true)
    try {
      await quizApi.remove(confirmTarget.id)
      setToast({ type: 'success', message: `Đã xoá quiz "${confirmTarget.title}".` })
      setConfirmTarget(null)
      await loadData()
    } catch (error) {
      setToast({ type: 'error', message: error.message })
    } finally {
      setDeleting(false)
    }
  }

  const totalQuestions = quizzes.reduce((sum, quiz) => sum + quiz.questionCount, 0)

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__brand">
          <div className="app-header__logo" aria-hidden="true">
            Q
          </div>
          <div>
            <h1>Quản lý bài Quiz</h1>
            <p>LearnHub LMS · Course Service</p>
          </div>
        </div>
        <div className="app-header__actions">
          <button type="button" className="button button--ghost" onClick={loadData} disabled={loading}>
            ⟳ Làm mới
          </button>
          <button
            type="button"
            className="button button--primary"
            onClick={() => setEditor({ mode: 'create' })}
            disabled={openingEditor || loading}
          >
            + Tạo quiz
          </button>
        </div>
      </header>

      <section className="stats" aria-label="Thống kê">
        <div className="stat">
          <span className="stat__value">{quizzes.length}</span>
          <span className="stat__label">Bài quiz</span>
        </div>
        <div className="stat">
          <span className="stat__value">{totalQuestions}</span>
          <span className="stat__label">Câu hỏi</span>
        </div>
        <div className="stat">
          <span className="stat__value">{lessonOptions.filter((lesson) => lesson.quizId == null).length}</span>
          <span className="stat__label">Bài học còn trống</span>
        </div>
      </section>

      {listError && (
        <div className="alert alert--error" role="alert">
          <strong>{listError.message}</strong>
          <button type="button" className="button button--ghost button--small" onClick={loadData}>
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
          <h2>Chưa có bài quiz nào</h2>
          <p>Tạo quiz đầu tiên cho một bài học loại QUIZ để bắt đầu.</p>
          <button type="button" className="button button--primary" onClick={() => setEditor({ mode: 'create' })}>
            + Tạo quiz đầu tiên
          </button>
        </div>
      ) : (
        <div className="quiz-grid">
          {quizzes.map((quiz) => (
            <article className="quiz-card" key={quiz.id}>
              <header className="quiz-card__header">
                <span className="badge">Quiz #{quiz.id}</span>
                <span className="quiz-card__score">{Number(quiz.passingScore)}% để đạt</span>
              </header>
              <h2 className="quiz-card__title">{quiz.title}</h2>
              <p className="quiz-card__lesson">{quiz.lessonTitle}</p>
              <p className="quiz-card__course">{quiz.courseTitle}</p>

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
                <button
                  type="button"
                  className="button button--ghost"
                  onClick={() => openEdit(quiz)}
                  disabled={openingEditor}
                >
                  Sửa
                </button>
                <button type="button" className="button button--danger-ghost" onClick={() => setConfirmTarget(quiz)}>
                  Xoá
                </button>
              </footer>
            </article>
          ))}
        </div>
      )}

      {editor && (
        <QuizForm
          key={editor.mode === 'edit' ? `edit-${editor.quiz.id}` : 'create'}
          quiz={editor.mode === 'edit' ? editor.quiz : null}
          lessonOptions={lessonOptions}
          onSubmit={saveQuiz}
          onCancel={() => setEditor(null)}
        />
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
