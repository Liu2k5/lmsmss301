import { useEffect, useRef, useState } from 'react'

const QUESTION_TYPES = [
  { value: 'SINGLE_CHOICE', label: 'Một đáp án đúng' },
  { value: 'MULTIPLE_CHOICE', label: 'Nhiều đáp án đúng' },
  { value: 'TRUE_FALSE', label: 'Đúng / Sai' },
]

function newOption(correct = false) {
  return { content: '', correct }
}

function newQuestion() {
  return {
    content: '',
    type: 'SINGLE_CHOICE',
    options: [newOption(true), newOption(false)],
  }
}

function toFormState(quiz) {
  if (!quiz) {
    return {
      lessonId: '',
      title: '',
      description: '',
      passingScore: '70',
      questions: [newQuestion()],
    }
  }
  return {
    lessonId: String(quiz.lessonId ?? ''),
    title: quiz.title ?? '',
    description: quiz.description ?? '',
    passingScore: String(quiz.passingScore ?? '70'),
    questions: (quiz.questions ?? []).map((question) => ({
      id: question.id,
      content: question.content,
      type: question.type,
      options: (question.options ?? []).map((option) => ({
        id: option.id,
        content: option.content,
        correct: option.correct,
      })),
    })),
  }
}

function validate(form) {
  const messages = []
  if (!form.lessonId) {
    messages.push('Chọn bài học cho quiz.')
  }
  if (!form.title.trim()) {
    messages.push('Tên quiz không được để trống.')
  }
  const score = Number(form.passingScore)
  if (Number.isNaN(score) || score < 0 || score > 100) {
    messages.push('Điểm đạt phải nằm trong khoảng 0 – 100.')
  }
  if (form.questions.length === 0) {
    messages.push('Quiz phải có ít nhất 1 câu hỏi.')
  }

  form.questions.forEach((question, questionIndex) => {
    const order = questionIndex + 1
    if (!question.content.trim()) {
      messages.push(`Câu hỏi ${order}: nội dung không được để trống.`)
    }
    if (question.options.length === 0) {
      messages.push(`Câu hỏi ${order}: phải có ít nhất 1 đáp án.`)
    }
    if (question.options.some((option) => !option.content.trim())) {
      messages.push(`Câu hỏi ${order}: nội dung đáp án không được để trống.`)
    }
    const correctCount = question.options.filter((option) => option.correct).length
    if (correctCount === 0) {
      messages.push(`Câu hỏi ${order}: phải có ít nhất 1 đáp án đúng.`)
    }
    if (question.type !== 'MULTIPLE_CHOICE' && correctCount > 1) {
      messages.push(`Câu hỏi ${order}: loại này chỉ được có 1 đáp án đúng.`)
    }
  })

  return messages
}

function toPayload(form) {
  return {
    lessonId: Number(form.lessonId),
    title: form.title.trim(),
    description: form.description.trim() || null,
    passingScore: Number(form.passingScore),
    questions: form.questions.map((question) => ({
      id: question.id ?? null,
      content: question.content.trim(),
      type: question.type,
      options: question.options.map((option) => ({
        id: option.id ?? null,
        content: option.content.trim(),
        correct: option.correct,
      })),
    })),
  }
}

export default function QuizForm({ quiz, lessonOptions, onSubmit, onCancel }) {
  const isEditing = Boolean(quiz)
  const [form, setForm] = useState(() => toFormState(quiz))
  const [errors, setErrors] = useState([])
  const [serverError, setServerError] = useState(null)
  const [saving, setSaving] = useState(false)
  const titleRef = useRef(null)

  useEffect(() => {
    titleRef.current?.focus()
  }, [])

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !saving) {
        onCancel()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onCancel, saving])

  function patchForm(patch) {
    setForm((current) => ({ ...current, ...patch }))
  }

  function patchQuestion(index, patch) {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, i) => (i === index ? { ...question, ...patch } : question)),
    }))
  }

  function patchOption(questionIndex, optionIndex, patch) {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, i) => {
        if (i !== questionIndex) {
          return question
        }
        return {
          ...question,
          options: question.options.map((option, oi) => (oi === optionIndex ? { ...option, ...patch } : option)),
        }
      }),
    }))
  }

  function markCorrect(questionIndex, optionIndex) {
    const question = form.questions[questionIndex]
    const multiple = question.type === 'MULTIPLE_CHOICE'
    setForm((current) => ({
      ...current,
      questions: current.questions.map((item, i) => {
        if (i !== questionIndex) {
          return item
        }
        return {
          ...item,
          options: item.options.map((option, oi) => {
            if (multiple) {
              return oi === optionIndex ? { ...option, correct: !option.correct } : option
            }
            return { ...option, correct: oi === optionIndex }
          }),
        }
      }),
    }))
  }

  function changeType(questionIndex, type) {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, i) => {
        if (i !== questionIndex) {
          return question
        }
        if (type === 'TRUE_FALSE') {
          return {
            ...question,
            type,
            options: [
              { content: 'Đúng', correct: true },
              { content: 'Sai', correct: false },
            ],
          }
        }
        const options = (question.options.length ? question.options : [newOption(true), newOption(false)])
          .map((option, index) => ({ ...option, correct: index === 0 }))
        return { ...question, type, options }
      }),
    }))
  }

  function addQuestion() {
    setForm((current) => ({ ...current, questions: [...current.questions, newQuestion()] }))
  }

  function removeQuestion(index) {
    setForm((current) => ({
      ...current,
      questions: current.questions.filter((_, i) => i !== index),
    }))
  }

  function addOption(questionIndex) {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, i) =>
        i === questionIndex ? { ...question, options: [...question.options, newOption(false)] } : question,
      ),
    }))
  }

  function removeOption(questionIndex, optionIndex) {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, i) => {
        if (i !== questionIndex || question.options.length <= 1) {
          return question
        }
        return { ...question, options: question.options.filter((_, oi) => oi !== optionIndex) }
      }),
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const validationErrors = validate(form)
    setErrors(validationErrors)
    setServerError(null)
    if (validationErrors.length > 0) {
      return
    }

    setSaving(true)
    try {
      await onSubmit(toPayload(form))
    } catch (error) {
      setServerError(error)
    } finally {
      setSaving(false)
    }
  }

  const availableLessons = lessonOptions.filter(
    (lesson) => lesson.quizId == null || (quiz && lesson.id === quiz.lessonId),
  )

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="quiz-form-title">
        <header className="modal__header">
          <div>
            <h2 id="quiz-form-title">{isEditing ? 'Sửa bài quiz' : 'Tạo bài quiz'}</h2>
            <p className="modal__subtitle">
              {isEditing
                ? 'Cập nhật thông tin, câu hỏi và đáp án của quiz.'
                : 'Mỗi bài học loại QUIZ chỉ có thể gắn một quiz.'}
            </p>
          </div>
          <button type="button" className="icon-button" onClick={onCancel} aria-label="Đóng" disabled={saving}>
            ✕
          </button>
        </header>

        <form className="modal__body" onSubmit={handleSubmit} noValidate>
          <div className="field-grid">
            <label className="field field--wide">
              <span className="field__label">Bài học</span>
              <select
                value={form.lessonId}
                onChange={(event) => patchForm({ lessonId: event.target.value })}
                disabled={saving}
              >
                <option value="">— Chọn bài học loại QUIZ —</option>
                {availableLessons.map((lesson) => (
                  <option key={lesson.id} value={lesson.id}>
                    {lesson.courseTitle} › {lesson.sectionTitle} › {lesson.title}
                  </option>
                ))}
              </select>
              {availableLessons.length === 0 && (
                <span className="field__hint">
                  Không còn bài học QUIZ nào trống. Hãy tạo bài học loại QUIZ trước.
                </span>
              )}
            </label>

            <label className="field field--wide">
              <span className="field__label">Tên quiz</span>
              <input
                ref={titleRef}
                type="text"
                value={form.title}
                maxLength={255}
                placeholder="Ví dụ: Kiểm tra cuối chương 1"
                onChange={(event) => patchForm({ title: event.target.value })}
                disabled={saving}
              />
            </label>

            <label className="field">
              <span className="field__label">Điểm đạt (%)</span>
              <input
                type="number"
                min="0"
                max="100"
                step="0.5"
                value={form.passingScore}
                onChange={(event) => patchForm({ passingScore: event.target.value })}
                disabled={saving}
              />
            </label>

            <label className="field field--wide">
              <span className="field__label">Mô tả</span>
              <textarea
                rows={2}
                value={form.description}
                placeholder="Mô tả ngắn cho quiz (không bắt buộc)"
                onChange={(event) => patchForm({ description: event.target.value })}
                disabled={saving}
              />
            </label>
          </div>

          <div className="questions-header">
            <h3>Câu hỏi ({form.questions.length})</h3>
            <button type="button" className="button button--ghost" onClick={addQuestion} disabled={saving}>
              + Thêm câu hỏi
            </button>
          </div>

          <div className="questions">
            {form.questions.map((question, questionIndex) => (
              <section className="question-card" key={question.id ?? `q-${questionIndex}`}>
                <header className="question-card__header">
                  <span className="question-card__index">Câu {questionIndex + 1}</span>
                  <select
                    value={question.type}
                    onChange={(event) => changeType(questionIndex, event.target.value)}
                    disabled={saving}
                    aria-label={`Loại câu hỏi ${questionIndex + 1}`}
                  >
                    {QUESTION_TYPES.map((type) => (
                      <option key={type.value} value={type.value}>
                        {type.label}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="button button--danger-ghost"
                    onClick={() => removeQuestion(questionIndex)}
                    disabled={saving || form.questions.length === 1}
                    title={form.questions.length === 1 ? 'Quiz phải có ít nhất 1 câu hỏi' : 'Xoá câu hỏi'}
                  >
                    Xoá câu
                  </button>
                </header>

                <textarea
                  className="question-card__content"
                  rows={2}
                  value={question.content}
                  placeholder="Nội dung câu hỏi"
                  onChange={(event) => patchQuestion(questionIndex, { content: event.target.value })}
                  disabled={saving}
                />

                <div className="options">
                  {question.options.map((option, optionIndex) => (
                    <div className="option-row" key={option.id ?? `o-${optionIndex}`}>
                      <input
                        className="option-row__mark"
                        type={question.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'}
                        name={`correct-${questionIndex}`}
                        checked={option.correct}
                        onChange={() => markCorrect(questionIndex, optionIndex)}
                        disabled={saving}
                        aria-label={`Đánh dấu đáp án ${optionIndex + 1} là đúng`}
                      />
                      <input
                        type="text"
                        value={option.content}
                        placeholder={`Đáp án ${optionIndex + 1}`}
                        onChange={(event) =>
                          patchOption(questionIndex, optionIndex, { content: event.target.value })
                        }
                        disabled={saving}
                      />
                      <button
                        type="button"
                        className="icon-button icon-button--small"
                        onClick={() => removeOption(questionIndex, optionIndex)}
                        disabled={saving || question.options.length <= 1}
                        aria-label={`Xoá đáp án ${optionIndex + 1}`}
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>

                <button
                  type="button"
                  className="button button--ghost button--small"
                  onClick={() => addOption(questionIndex)}
                  disabled={saving || question.type === 'TRUE_FALSE'}
                >
                  + Thêm đáp án
                </button>
              </section>
            ))}
          </div>

          {errors.length > 0 && (
            <div className="alert alert--warning" role="alert">
              <strong>Vui lòng kiểm tra lại:</strong>
              <ul>
                {errors.map((message) => (
                  <li key={message}>{message}</li>
                ))}
              </ul>
            </div>
          )}

          {serverError && (
            <div className="alert alert--error" role="alert">
              <strong>{serverError.message}</strong>
              {serverError.details?.length > 0 && (
                <ul>
                  {serverError.details.map((detail) => (
                    <li key={`${detail.field}-${detail.message}`}>
                      <code>{detail.field}</code>: {detail.message}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          <footer className="modal__footer">
            <button type="button" className="button button--ghost" onClick={onCancel} disabled={saving}>
              Huỷ
            </button>
            <button type="submit" className="button button--primary" disabled={saving}>
              {saving ? 'Đang lưu…' : isEditing ? 'Lưu thay đổi' : 'Tạo quiz'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  )
}
