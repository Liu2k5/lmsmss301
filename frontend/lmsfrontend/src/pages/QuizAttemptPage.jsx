import { useCallback, useEffect, useMemo, useState } from 'react'
import { quizApi } from '../api/quizApi.js'
import { progressApi } from '../api/progressApi.js'
import { enrollmentApi } from '../api/enrollmentApi.js'
import { useAuth } from '../auth/AuthContext.js'

const TYPE_LABEL = {
  SINGLE_CHOICE: 'Chọn một đáp án',
  MULTIPLE_CHOICE: 'Chọn tất cả đáp án đúng',
  TRUE_FALSE: 'Đúng / Sai',
}

/**
 * UC-P04 — Học viên làm bài quiz và nộp bài.
 *
 * Mở từ danh sách quiz: `#/quiz-attempt?quizId=<uuid>&courseId=<uuid>`.
 * Đề lấy từ `GET /quizzes/{id}/preview` (không có đáp án đúng); điểm do Progress Service chấm.
 */
export default function QuizAttemptPage({ quizId, courseId }) {
  const { ready, authenticated, user } = useAuth()
  const studentId = user?.id ?? null

  const [quiz, setQuiz] = useState(null)
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [enrolling, setEnrolling] = useState(false)
  const [enrolledNow, setEnrolledNow] = useState(false)
  const [error, setError] = useState(null)
  // 403 = chưa đăng ký khoá học -> gợi ý đăng ký ngay (UC-E01).
  const needsEnrollment = error?.status === 403

  const loadHistory = useCallback(async () => {
    if (!studentId || !courseId || !quizId) return []
    try {
      return await progressApi.quizAttempts(courseId, quizId, studentId)
    } catch {
      return []
    }
  }, [courseId, quizId, studentId])

  useEffect(() => {
    if (!ready || !authenticated || !quizId) {
      return undefined
    }
    let active = true
    quizApi
      .preview(quizId)
      .then((data) => {
        if (active) {
          setQuiz(data)
          setError(null)
        }
      })
      .catch((requestError) => active && setError(requestError))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [ready, authenticated, quizId])

  useEffect(() => {
    let active = true
    loadHistory().then((rows) => active && setHistory(rows))
    return () => {
      active = false
    }
  }, [loadHistory])

  const attemptsUsed = history.length
  const maxAttempts = quiz?.maxAttempts ?? null
  const attemptsLeft = maxAttempts == null ? null : Math.max(0, maxAttempts - attemptsUsed)
  const outOfAttempts = attemptsLeft === 0 && !result

  function toggleOption(question, optionId) {
    setAnswers((current) => {
      const chosen = new Set(current[question.id] ?? [])
      if (question.type === 'MULTIPLE_CHOICE') {
        if (chosen.has(optionId)) chosen.delete(optionId)
        else chosen.add(optionId)
      } else {
        chosen.clear()
        chosen.add(optionId)
      }
      return { ...current, [question.id]: Array.from(chosen) }
    })
  }

  async function submit(event) {
    event.preventDefault()
    if (!studentId || !courseId) return
    setSubmitting(true)
    setError(null)
    try {
      const payload = {
        studentId,
        answers: (quiz.questions ?? []).map((question) => ({
          questionId: question.id,
          optionIds: answers[question.id] ?? [],
        })),
      }
      const submitted = await progressApi.submitQuiz(courseId, quizId, payload)
      setResult(submitted)
      setHistory(await loadHistory())
    } catch (requestError) {
      setError(requestError)
    } finally {
      setSubmitting(false)
    }
  }

  function retry() {
    setResult(null)
    setAnswers({})
    setError(null)
  }

  async function enroll() {
    setEnrolling(true)
    setError(null)
    try {
      await enrollmentApi.enroll(courseId)
      setEnrolledNow(true)
      // Progress Service tạo tiến độ từ event StudentEnrolled; chờ một nhịp rồi tải lại lịch sử.
      await new Promise((resolve) => { setTimeout(resolve, 1500) })
      setHistory(await loadHistory())
    } catch (requestError) {
      setError(requestError)
    } finally {
      setEnrolling(false)
    }
  }

  const answeredCount = useMemo(
    () => (quiz?.questions ?? []).filter((question) => (answers[question.id] ?? []).length > 0).length,
    [quiz, answers],
  )

  if (!ready) {
    return <div className="app app--message"><p>Đang kiểm tra đăng nhập…</p></div>
  }

  if (!authenticated) {
    return (
      <div className="app app--message">
        <h1>Làm bài quiz</h1>
        <p>Vui lòng đăng nhập bằng tài khoản học viên để làm bài.</p>
        <a className="button button--primary" href="#/login">Đăng nhập</a>
      </div>
    )
  }

  if (!quizId || !courseId) {
    return (
      <div className="app app--message">
        <h1>Làm bài quiz</h1>
        <p>Thiếu thông tin quiz hoặc khoá học.</p>
        <a className="button button--ghost" href="#/quizzes">← Danh sách quiz</a>
      </div>
    )
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__brand">
          <div className="app-header__logo" aria-hidden="true">?</div>
          <div>
            <h1>{quiz?.title ?? 'Làm bài quiz'}</h1>
            <p>{quiz ? `${quiz.courseTitle} · ${quiz.lessonTitle}` : 'Đang tải đề…'}</p>
          </div>
        </div>
        <div className="app-header__actions">
          <a className="button button--ghost" href="#/quizzes">← Danh sách quiz</a>
        </div>
      </header>

      {loading && <p className="muted">Đang tải đề…</p>}

      {error && (
        <div className="alert alert--error" role="alert">
          <strong>{error.message}</strong>
          {needsEnrollment && (
            <button type="button" className="button button--primary button--small" onClick={enroll}
              disabled={enrolling}>
              {enrolling ? 'Đang đăng ký…' : `Đăng ký khoá học`}
            </button>
          )}
        </div>
      )}

      {enrolledNow && (
        <div className="alert alert--success" role="status">
          <strong>Đã đăng ký khoá học.</strong> Bấm “Nộp bài” lại để ghi nhận lượt làm.
        </div>
      )}

      {quiz && (
        <>
          <section className="stats" aria-label="Thông tin bài làm">
            <div className="stat">
              <span className="stat__value">{Number(quiz.passingScore)}%</span>
              <span className="stat__label">Điểm đạt</span>
            </div>
            <div className="stat">
              <span className="stat__value">{quiz.questions.length}</span>
              <span className="stat__label">Câu hỏi</span>
            </div>
            <div className="stat">
              <span className="stat__value">{maxAttempts ?? '∞'}</span>
              <span className="stat__label">Số lần tối đa</span>
            </div>
            <div className="stat">
              <span className="stat__value">{attemptsUsed}</span>
              <span className="stat__label">Đã làm</span>
            </div>
          </section>

          {result ? (
            <section className={`quiz-result quiz-result--${result.passed ? 'pass' : 'fail'}`} aria-live="polite">
              <h2>{result.passed ? 'Đạt bài kiểm tra 🎉' : 'Chưa đạt bài kiểm tra'}</h2>
              <p>
                Lượt {result.attemptNumber}: đúng {result.correctCount}/{result.totalQuestions} câu —
                <strong> {Number(result.score)}%</strong> (cần {Number(result.passingScore)}%).
              </p>
              {result.lessonCompleted && <p className="muted">Bài học đã được đánh dấu hoàn thành.</p>}
              <p className="muted">
                {result.remainingAttempts < 0
                  ? 'Không giới hạn số lần làm.'
                  : `Còn ${result.remainingAttempts} lượt.`}
              </p>
              <div className="quiz-result__actions">
                {result.remainingAttempts !== 0 && (
                  <button type="button" className="button button--primary" onClick={retry}>
                    Làm lại
                  </button>
                )}
                <a className="button button--ghost" href="#/quizzes">Về danh sách quiz</a>
              </div>
            </section>
          ) : outOfAttempts ? (
            <div className="alert alert--warning" role="status">
              <strong>Bạn đã dùng hết {maxAttempts} lần làm quiz này.</strong>
            </div>
          ) : (
            <form className="quiz-attempt" onSubmit={submit}>
              <ol className="preview-questions">
                {(quiz.questions ?? []).map((question, index) => (
                  <li className="preview-question" key={question.id}>
                    <div className="preview-question__head">
                      <span className="badge">Câu {index + 1}</span>
                      <span className="tag">{TYPE_LABEL[question.type] ?? question.type}</span>
                    </div>
                    <p className="preview-question__content">{question.content}</p>
                    <ul className="preview-options">
                      {(question.options ?? []).map((option) => {
                        const checked = (answers[question.id] ?? []).includes(option.id)
                        return (
                          <li key={option.id}>
                            <label className={`preview-option${checked ? ' preview-option--correct' : ''}`}>
                              <input
                                type={question.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'}
                                name={question.id}
                                checked={checked}
                                onChange={() => toggleOption(question, option.id)}
                                disabled={submitting}
                              />
                              <span>{option.content}</span>
                            </label>
                          </li>
                        )
                      })}
                    </ul>
                  </li>
                ))}
              </ol>

              <footer className="quiz-attempt__footer">
                <span className="muted">Đã trả lời {answeredCount}/{quiz.questions.length} câu</span>
                <button type="submit" className="button button--primary" disabled={submitting}>
                  {submitting ? 'Đang nộp…' : 'Nộp bài'}
                </button>
              </footer>
            </form>
          )}

          {history.length > 0 && (
            <section className="quiz-history" aria-label="Lịch sử làm bài">
              <h2>Lịch sử làm bài</h2>
              <table>
                <thead>
                  <tr>
                    <th>Lượt</th>
                    <th>Điểm</th>
                    <th>Kết quả</th>
                    <th>Thời điểm</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((attempt) => (
                    <tr key={attempt.id}>
                      <td>{attempt.attemptNumber}</td>
                      <td>{Number(attempt.score)}%</td>
                      <td>{attempt.passed ? 'Đạt' : 'Chưa đạt'}</td>
                      <td>{attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleString('vi-VN') : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
        </>
      )}
    </div>
  )
}
