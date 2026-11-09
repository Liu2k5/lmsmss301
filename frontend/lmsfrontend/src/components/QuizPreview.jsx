import { useEffect, useState } from 'react'
import { quizApi } from '../api/quizApi.js'

const TYPE_LABEL = {
  SINGLE_CHOICE: 'Một đáp án đúng',
  MULTIPLE_CHOICE: 'Nhiều đáp án đúng',
  TRUE_FALSE: 'Đúng / Sai',
}

/**
 * Xem trước quiz đúng như học viên sẽ thấy (không có đáp án đúng).
 * Khi giảng viên bấm "Hiện đáp án", tải thêm bản đầy đủ để tô đáp án đúng.
 */
export default function QuizPreview({ quizId, onClose }) {
  const [preview, setPreview] = useState(null)
  const [answerKey, setAnswerKey] = useState(null)
  const [showAnswers, setShowAnswers] = useState(false)
  const [loadingAnswers, setLoadingAnswers] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    quizApi
      .preview(quizId)
      .then((data) => {
        if (active) {
          setPreview(data)
        }
      })
      .catch((err) => {
        if (active) {
          setError(err)
        }
      })
    return () => {
      active = false
    }
  }, [quizId])

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  async function toggleAnswers() {
    if (showAnswers) {
      setShowAnswers(false)
      return
    }
    if (!answerKey) {
      setLoadingAnswers(true)
      try {
        setAnswerKey(await quizApi.get(quizId))
        setShowAnswers(true)
      } catch (err) {
        setError(err)
      } finally {
        setLoadingAnswers(false)
      }
    } else {
      setShowAnswers(true)
    }
  }

  function isCorrect(questionIndex, optionId) {
    if (!showAnswers || !answerKey) {
      return false
    }
    const source = answerKey.questions?.[questionIndex]
    return Boolean(source?.options?.find((option) => option.id === optionId)?.correct)
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal modal--preview" role="dialog" aria-modal="true" aria-labelledby="quiz-preview-title">
        <header className="modal__header">
          <div>
            <h2 id="quiz-preview-title">Xem trước: {preview?.title ?? '…'}</h2>
            <p className="modal__subtitle">
              {preview
                ? `${preview.courseTitle} › ${preview.sectionTitle} › ${preview.lessonTitle}`
                : 'Đang tải…'}
            </p>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Đóng">
            ✕
          </button>
        </header>

        <div className="modal__body">
          {error && (
            <div className="alert alert--error" role="alert">
              {error.message}
            </div>
          )}

          {!preview && !error && (
            <div className="state-card">
              <span className="spinner" aria-hidden="true" />
              <p>Đang tải bản xem trước…</p>
            </div>
          )}

          {preview && (
            <>
              <div className="preview-meta">
                <span className="badge">{preview.questions.length} câu hỏi</span>
                <span className="quiz-card__score">{Number(preview.passingScore)}% để đạt</span>
                {preview.maxAttempts && (
                  <span className="badge">Tối đa {preview.maxAttempts} lần làm</span>
                )}
                <button
                  type="button"
                  className="button button--ghost button--small preview-meta__toggle"
                  onClick={toggleAnswers}
                  disabled={loadingAnswers}
                >
                  {loadingAnswers ? 'Đang tải…' : showAnswers ? 'Ẩn đáp án' : 'Hiện đáp án'}
                </button>
              </div>

              {preview.description && <p className="preview-description">{preview.description}</p>}

              <ol className="preview-questions">
                {preview.questions.map((question, questionIndex) => (
                  <li className="preview-question" key={question.id}>
                    <div className="preview-question__head">
                      <span className="question-card__index">Câu {questionIndex + 1}</span>
                      <span className="tag">{TYPE_LABEL[question.type] ?? question.type}</span>
                    </div>
                    <p className="preview-question__content">{question.content}</p>
                    <ul className="preview-options">
                      {question.options.map((option, optionIndex) => {
                        const correct = isCorrect(questionIndex, option.id)
                        return (
                          <li
                            key={option.id}
                            className={`preview-option${correct ? ' preview-option--correct' : ''}`}
                          >
                            <span className="preview-option__mark" aria-hidden="true">
                              {question.type === 'MULTIPLE_CHOICE'
                                ? String.fromCharCode(65 + optionIndex)
                                : String.fromCharCode(65 + optionIndex)}
                            </span>
                            <span>{option.content}</span>
                            {correct && <span className="tag tag--success">đáp án đúng</span>}
                          </li>
                        )
                      })}
                    </ul>
                  </li>
                ))}
              </ol>
            </>
          )}
        </div>

        <footer className="modal__footer">
          <button type="button" className="button button--ghost" onClick={onClose}>
            Đóng
          </button>
        </footer>
      </div>
    </div>
  )
}
