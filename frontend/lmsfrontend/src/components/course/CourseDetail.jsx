import { useCallback, useEffect, useState } from 'react'
import { courseApi } from '../../api/courseApi.js'
import { useAuth } from '../../auth/AuthContext.js'
import StatusBadge from './StatusBadge.jsx'
import {
  CONFIRM_TRANSITIONS,
  DELETABLE_STATUSES,
  MATERIAL_TYPES,
  STATUS_META,
  formatBytes,
  formatDate,
  lessonTypeMeta,
  transitionLabel,
} from './courseMeta.js'
import { COURSE_FIELDS, LESSON_FIELDS, MATERIAL_FIELDS, SECTION_FIELDS, UPLOADED_MATERIAL_FIELDS } from './formFields.js'

function moved(ids, index, delta) {
  const next = [...ids]
  const target = index + delta
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}

export default function CourseDetail({ courseId, onBack, openDialog, closeDialog, notify }) {
  // Chỉ giảng viên/admin được sửa nội dung; học viên và khách chỉ xem.
  const { authenticated, isInstructor, isAdmin } = useAuth()
  const canManage = authenticated && (isInstructor || isAdmin)

  const [course, setCourse] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [expanded, setExpanded] = useState(() => new Set())
  const [playingId, setPlayingId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      setCourse(await courseApi.get(courseId))
    } catch (error) {
      setLoadError(error)
    } finally {
      setLoading(false)
    }
  }, [courseId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chi tiết khi mở khoá học
    load()
  }, [load])

  /** Chạy một thao tác trả về chi tiết khoá học mới; báo lỗi bằng toast. */
  async function run(action, successMessage) {
    setBusy(true)
    try {
      const detail = await action()
      if (detail) setCourse(detail)
      if (successMessage) notify('success', successMessage)
    } catch (error) {
      notify('error', error.message)
    } finally {
      setBusy(false)
    }
  }

  /** Dùng cho form: lỗi được ném lại để form hiển thị ngay trong modal. */
  function formSubmit(action, successMessage) {
    return async (payload) => {
      const detail = await action(payload)
      setCourse(detail)
      closeDialog()
      notify('success', successMessage)
    }
  }

  function toggle(lessonId) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(lessonId)) next.delete(lessonId)
      else next.add(lessonId)
      return next
    })
  }

  if (loading && !course) {
    return (
      <div className="state-card">
        <span className="spinner" aria-hidden="true" />
        <p>Đang tải khoá học…</p>
      </div>
    )
  }

  if (loadError && !course) {
    return (
      <>
        <button type="button" className="back-link" onClick={onBack}>
          ← Danh sách khoá học
        </button>
        <div className="alert alert--error" role="alert">
          <strong>{loadError.message}</strong>
          <button type="button" className="button button--ghost button--small" onClick={load}>
            Thử lại
          </button>
        </div>
      </>
    )
  }

  const editable = course.editable && canManage
  const totalLessons = course.sections.reduce((sum, section) => sum + section.lessons.length, 0)
  const totalMaterials = course.sections.reduce(
    (sum, section) => sum + section.lessons.reduce((acc, lesson) => acc + lesson.materials.length, 0),
    0,
  )
  const missingQuiz = course.sections.flatMap((section) =>
    section.lessons.filter((lesson) => lesson.type === 'QUIZ' && lesson.quizId == null),
  )

  // ---------------------------------------------------------------- course

  function editCourse() {
    openDialog({
      kind: 'form',
      title: 'Sửa thông tin khoá học',
      fields: COURSE_FIELDS,
      initialValues: { ...course, thumbnail: course.thumbnailUrl },
      onSubmit: formSubmit(async ({ thumbnail, ...info }) => {
        // thumbnailUrl để trống = backend giữ nguyên ảnh bìa; đổi / bỏ ảnh đi qua endpoint riêng.
        let detail = await courseApi.update(course.id, { ...info, thumbnailUrl: null })
        if (thumbnail?.file) {
          detail = await courseApi.uploadThumbnail(course.id, thumbnail.file)
        } else if (thumbnail?.removed) {
          detail = await courseApi.removeThumbnail(course.id)
        }
        return detail
      }, 'Đã cập nhật khoá học.'),
    })
  }

  function deleteCourse() {
    openDialog({
      kind: 'confirm',
      title: 'Xoá khoá học?',
      message: `Khoá học "${course.title}" cùng toàn bộ chương, bài học, học liệu và quiz sẽ bị xoá vĩnh viễn.`,
      confirmLabel: 'Xoá khoá học',
      onConfirm: async () => {
        try {
          await courseApi.remove(course.id)
          closeDialog()
          notify('success', `Đã xoá khoá học "${course.title}".`)
          onBack()
        } catch (error) {
          closeDialog()
          notify('error', error.message)
        }
      },
    })
  }

  function changeStatus(target) {
    const label = transitionLabel(course.status, target)
    const action = () =>
      run(() => courseApi.changeStatus(course.id, target), `${label}: khoá học chuyển sang "${STATUS_META[target].label}".`)

    if (!CONFIRM_TRANSITIONS.has(target)) {
      action()
      return
    }
    openDialog({
      kind: 'confirm',
      title: `${label}?`,
      message:
        target === 'ARCHIVED'
          ? 'Khoá học lưu trữ sẽ chỉ đọc. Có thể khôi phục về bản nháp sau.'
          : 'Học viên mới sẽ không thấy khoá học này nữa.',
      confirmLabel: label,
      danger: target !== 'UNPUBLISHED',
      onConfirm: async () => {
        closeDialog()
        await action()
      },
    })
  }

  // --------------------------------------------------------------- sections

  function addSection() {
    openDialog({
      kind: 'form',
      title: 'Thêm chương',
      fields: SECTION_FIELDS,
      initialValues: { title: `Chương ${course.sections.length + 1}: ` },
      submitLabel: 'Thêm chương',
      onSubmit: formSubmit((payload) => courseApi.addSection(course.id, payload), 'Đã thêm chương.'),
    })
  }

  function editSection(section) {
    openDialog({
      kind: 'form',
      title: 'Sửa chương',
      fields: SECTION_FIELDS,
      initialValues: section,
      onSubmit: formSubmit((payload) => courseApi.updateSection(section.id, payload), 'Đã cập nhật chương.'),
    })
  }

  function deleteSection(section) {
    openDialog({
      kind: 'confirm',
      title: 'Xoá chương?',
      message: `Chương "${section.title}" và ${section.lessons.length} bài học bên trong (kèm học liệu, quiz) sẽ bị xoá.`,
      confirmLabel: 'Xoá chương',
      onConfirm: async () => {
        closeDialog()
        await run(() => courseApi.removeSection(section.id), 'Đã xoá chương.')
      },
    })
  }

  function moveSection(index, delta) {
    const ids = moved(course.sections.map((section) => section.id), index, delta)
    run(() => courseApi.reorderSections(course.id, ids))
  }

  // ---------------------------------------------------------------- lessons

  function addLesson(section) {
    openDialog({
      kind: 'form',
      title: 'Thêm bài học',
      subtitle: section.title,
      fields: LESSON_FIELDS,
      initialValues: { type: 'VIDEO', preview: false },
      submitLabel: 'Thêm bài học',
      onSubmit: formSubmit((payload) => courseApi.addLesson(section.id, payload), 'Đã thêm bài học.'),
    })
  }

  function editLesson(lesson) {
    openDialog({
      kind: 'form',
      title: 'Sửa bài học',
      fields: LESSON_FIELDS,
      initialValues: lesson,
      onSubmit: formSubmit((payload) => courseApi.updateLesson(lesson.id, payload), 'Đã cập nhật bài học.'),
    })
  }

  function deleteLesson(lesson) {
    const extras = []
    if (lesson.materials.length) extras.push(`${lesson.materials.length} học liệu`)
    if (lesson.quizId) extras.push('quiz đi kèm')
    openDialog({
      kind: 'confirm',
      title: 'Xoá bài học?',
      message: `Bài học "${lesson.title}"${extras.length ? ` cùng ${extras.join(' và ')}` : ''} sẽ bị xoá.`,
      confirmLabel: 'Xoá bài học',
      onConfirm: async () => {
        closeDialog()
        await run(() => courseApi.removeLesson(lesson.id), 'Đã xoá bài học.')
      },
    })
  }

  function moveLesson(section, index, delta) {
    const ids = moved(section.lessons.map((lesson) => lesson.id), index, delta)
    run(() => courseApi.reorderLessons(section.id, ids))
  }

  // -------------------------------------------------------------- materials

  function addMaterial(lesson) {
    openDialog({
      kind: 'form',
      title: 'Thêm học liệu dạng link',
      subtitle: lesson.title,
      fields: MATERIAL_FIELDS,
      initialValues: { type: lesson.type === 'VIDEO' ? 'VIDEO' : 'DOCUMENT' },
      submitLabel: 'Thêm link',
      onSubmit: formSubmit(async (payload) => {
        const detail = await courseApi.addMaterial(lesson.id, payload)
        setExpanded((current) => new Set(current).add(lesson.id))
        return detail
      }, 'Đã thêm học liệu.'),
    })
  }

  function uploadMaterial(lesson) {
    openDialog({
      kind: 'upload',
      subtitle: lesson.title,
      defaultType: lesson.type === 'VIDEO' ? 'VIDEO' : '',
      onUpload: (file, meta, onProgress) => {
        const upload = courseApi.uploadMaterial(lesson.id, file, meta, onProgress)
        return {
          abort: upload.abort,
          promise: upload.promise.then((detail) => {
            setCourse(detail)
            setExpanded((current) => new Set(current).add(lesson.id))
            closeDialog()
            notify('success', `Đã upload "${file.name}".`)
          }),
        }
      },
    })
  }

  function editMaterial(material) {
    openDialog({
      kind: 'form',
      title: 'Sửa học liệu',
      subtitle: material.uploaded ? 'File đang lưu trên MinIO' : undefined,
      fields: material.uploaded ? UPLOADED_MATERIAL_FIELDS : MATERIAL_FIELDS,
      initialValues: material,
      onSubmit: formSubmit((payload) => courseApi.updateMaterial(material.id, payload), 'Đã cập nhật học liệu.'),
    })
  }

  function deleteMaterial(material) {
    openDialog({
      kind: 'confirm',
      title: 'Xoá học liệu?',
      message: `Học liệu "${material.name}" sẽ bị xoá khỏi bài học.`,
      confirmLabel: 'Xoá học liệu',
      onConfirm: async () => {
        closeDialog()
        await run(() => courseApi.removeMaterial(material.id), 'Đã xoá học liệu.')
      },
    })
  }

  // ------------------------------------------------------------------ view

  return (
    <div className={`course-detail${busy ? ' is-busy' : ''}`} aria-busy={busy}>
      <button type="button" className="back-link" onClick={onBack}>
        ← Danh sách khoá học
      </button>

      <section className="detail-hero">
        <div className="detail-hero__thumb">
          {course.thumbnailUrl ? (
            <img src={course.thumbnailUrl} alt="" />
          ) : (
            <span aria-hidden="true">{course.title.slice(0, 1).toUpperCase()}</span>
          )}
        </div>
        <div className="detail-hero__info">
          <div className="detail-hero__top">
            <StatusBadge status={course.status} />
            <span className="muted">#{course.id.slice(0, 8)} · Giảng viên #{course.instructorId.slice(0, 8)}</span>
          </div>
          <h1>{course.title}</h1>
          {course.description && <p className="detail-hero__desc">{course.description}</p>}
          <p className="muted small">
            {course.sections.length} chương · {totalLessons} bài học · {totalMaterials} học liệu · Cập nhật{' '}
            {formatDate(course.updatedAt)}
          </p>
          <div className="detail-hero__actions">
            {canManage && (
              <button type="button" className="button button--ghost" onClick={editCourse}
                disabled={!editable || busy}>
                ✎ Sửa thông tin
              </button>
            )}
            {canManage && DELETABLE_STATUSES.has(course.status) && (
              <button type="button" className="button button--danger-ghost" onClick={deleteCourse} disabled={busy}>
                Xoá khoá học
              </button>
            )}
          </div>
        </div>
      </section>

      {canManage && (
      <section className="workflow" aria-label="Trạng thái khoá học">
        <div>
          <h2>Quy trình xuất bản</h2>
          <p className="muted small">
            Bản nháp → Đã xuất bản. Xuất bản yêu cầu mỗi chương có bài học và mỗi bài Quiz đã có quiz.
          </p>
        </div>
        <div className="workflow__actions">
          {course.allowedTransitions.map((target) => (
            <button
              key={target}
              type="button"
              className={`button ${
                target === 'PUBLISHED'
                  ? 'button--primary'
                  : target === 'ARCHIVED'
                    ? 'button--danger-ghost'
                    : 'button--ghost'
              }`}
              onClick={() => changeStatus(target)}
              disabled={busy}
            >
              {transitionLabel(course.status, target)}
            </button>
          ))}
        </div>
      </section>
      )}

      {!editable && canManage && (
        <div className="alert alert--warning" role="note">
          Khoá học đang lưu trữ nên chỉ xem được. Bấm <strong>Khôi phục</strong> để chỉnh sửa tiếp.
        </div>
      )}
      {canManage && editable && missingQuiz.length > 0 && (
        <div className="alert alert--warning" role="note">
          {missingQuiz.length} bài học loại Quiz chưa có quiz:{' '}
          {missingQuiz.map((lesson) => lesson.title).join(', ')}.{' '}
          <a href={`#/quizzes?lessonId=${missingQuiz[0].id}`}>Tạo quiz cho “{missingQuiz[0].title}” →</a>
        </div>
      )}

      <section className="curriculum">
        <header className="curriculum__header">
          <h2>Nội dung khoá học</h2>
          {editable && (
            <button type="button" className="button button--primary" onClick={addSection} disabled={busy}>
              + Thêm chương
            </button>
          )}
        </header>

        {course.sections.length === 0 ? (
          <div className="state-card state-card--compact">
            <p>Chưa có chương nào. Thêm chương đầu tiên để bắt đầu xây dựng nội dung.</p>
          </div>
        ) : (
          course.sections.map((section, sectionIndex) => (
            <article className="section-card" key={section.id}>
              <header className="section-card__header">
                <div className="section-card__title">
                  <span className="section-card__index">Chương {section.position}</span>
                  <h3>{section.title}</h3>
                  {section.description && <p className="muted small">{section.description}</p>}
                </div>
                {editable && (
                  <div className="row-actions">
                    <button type="button" className="icon-button icon-button--neutral" aria-label="Chuyển chương lên"
                      title="Lên" onClick={() => moveSection(sectionIndex, -1)} disabled={busy || sectionIndex === 0}>
                      ↑
                    </button>
                    <button type="button" className="icon-button icon-button--neutral" aria-label="Chuyển chương xuống"
                      title="Xuống" onClick={() => moveSection(sectionIndex, 1)}
                      disabled={busy || sectionIndex === course.sections.length - 1}>
                      ↓
                    </button>
                    <button type="button" className="icon-button icon-button--neutral" aria-label="Sửa chương"
                      title="Sửa" onClick={() => editSection(section)} disabled={busy}>
                      ✎
                    </button>
                    <button type="button" className="icon-button" aria-label="Xoá chương" title="Xoá"
                      onClick={() => deleteSection(section)} disabled={busy}>
                      🗑
                    </button>
                  </div>
                )}
              </header>

              <ol className="lesson-list">
                {section.lessons.map((lesson, lessonIndex) => {
                  const meta = lessonTypeMeta(lesson.type)
                  const isOpen = expanded.has(lesson.id)
                  return (
                    <li className="lesson" key={lesson.id}>
                      <div className="lesson__row">
                        <span className={`lesson__icon lesson__icon--${lesson.type.toLowerCase()}`} aria-hidden="true">
                          {meta.icon}
                        </span>
                        <div className="lesson__main">
                          <span className="lesson__title">
                            {lesson.position}. {lesson.title}
                          </span>
                          <span className="lesson__tags">
                            <span className="tag">{meta.label}</span>
                            {lesson.preview && <span className="tag tag--info">Xem thử</span>}
                            {lesson.type === 'QUIZ' &&
                              (lesson.quizId ? (
                                canManage ? (
                                  <a
                                    className="tag tag--success tag--link"
                                    href={`#/quizzes?quizId=${lesson.quizId}`}
                                    title="Sửa quiz của bài học này"
                                  >
                                    Đã có quiz · Sửa →
                                  </a>
                                ) : (
                                  <a
                                    className="tag tag--success tag--link"
                                    href={`#/quiz-attempt?quizId=${lesson.quizId}&courseId=${course.id}`}
                                    title="Làm bài quiz này"
                                  >
                                    Đã có quiz · Làm bài →
                                  </a>
                                )
                              ) : canManage ? (
                                <a
                                  className="tag tag--warning tag--link"
                                  href={`#/quizzes?lessonId=${lesson.id}`}
                                  title="Tạo quiz cho bài học này"
                                >
                                  Chưa có quiz · Tạo →
                                </a>
                              ) : (
                                <span className="tag">Chưa có quiz</span>
                              ))}
                            <button type="button" className="tag tag--button" onClick={() => toggle(lesson.id)}
                              aria-expanded={isOpen}>
                              {lesson.materials.length} học liệu {isOpen ? '▴' : '▾'}
                            </button>
                          </span>
                        </div>
                        {editable && (
                          <div className="row-actions">
                            <button type="button" className="icon-button icon-button--neutral icon-button--small"
                              aria-label="Chuyển bài học lên" title="Lên"
                              onClick={() => moveLesson(section, lessonIndex, -1)} disabled={busy || lessonIndex === 0}>
                              ↑
                            </button>
                            <button type="button" className="icon-button icon-button--neutral icon-button--small"
                              aria-label="Chuyển bài học xuống" title="Xuống"
                              onClick={() => moveLesson(section, lessonIndex, 1)}
                              disabled={busy || lessonIndex === section.lessons.length - 1}>
                              ↓
                            </button>
                            <button type="button" className="icon-button icon-button--neutral icon-button--small"
                              aria-label="Sửa bài học" title="Sửa" onClick={() => editLesson(lesson)} disabled={busy}>
                              ✎
                            </button>
                            <button type="button" className="icon-button icon-button--small" aria-label="Xoá bài học"
                              title="Xoá" onClick={() => deleteLesson(lesson)} disabled={busy}>
                              🗑
                            </button>
                          </div>
                        )}
                      </div>

                      {isOpen && (
                        <div className="materials">
                          {lesson.materials.length === 0 ? (
                            <p className="muted small">Chưa có học liệu.</p>
                          ) : (
                            <ul>
                              {lesson.materials.map((material) => {
                                const canPlay = material.url && material.type === 'VIDEO' && material.uploaded
                                const playing = playingId === material.id
                                return (
                                <li className="material" key={material.id}>
                                  <span className="tag">
                                    {MATERIAL_TYPES.find((type) => type.value === material.type)?.label ?? material.type}
                                  </span>
                                  {material.uploaded && <span className="tag tag--info" title="File lưu trên MinIO">MinIO</span>}
                                  {material.url ? (
                                    <a href={material.url} target="_blank" rel="noreferrer" className="material__name">
                                      {material.name}
                                    </a>
                                  ) : (
                                    <span className="material__name" title="Không tạo được link xem file (MinIO chưa chạy?)">
                                      {material.name} ⚠
                                    </span>
                                  )}
                                  <span className="muted small">
                                    {[formatBytes(material.fileSize), material.mimeType].filter(Boolean).join(' · ')}
                                  </span>
                                  {canPlay && (
                                    <button type="button" className="tag tag--button" aria-expanded={playing}
                                      onClick={() => setPlayingId(playing ? null : material.id)}>
                                      {playing ? '■ Đóng' : '▶ Xem'}
                                    </button>
                                  )}
                                  {editable && (
                                    <span className="row-actions">
                                      <button type="button" className="icon-button icon-button--neutral icon-button--small"
                                        aria-label="Sửa học liệu" onClick={() => editMaterial(material)} disabled={busy}>
                                        ✎
                                      </button>
                                      <button type="button" className="icon-button icon-button--small"
                                        aria-label="Xoá học liệu" onClick={() => deleteMaterial(material)} disabled={busy}>
                                        🗑
                                      </button>
                                    </span>
                                  )}
                                  {playing && (
                                    <video className="material__player" src={material.url} controls autoPlay preload="metadata">
                                      Trình duyệt không phát được video này.
                                    </video>
                                  )}
                                </li>
                                )
                              })}
                            </ul>
                          )}
                          {editable && (
                            <div className="materials__actions">
                              <button type="button" className="button button--primary button--small"
                                onClick={() => uploadMaterial(lesson)} disabled={busy}>
                                ⬆ Upload video / file
                              </button>
                              <button type="button" className="button button--ghost button--small"
                                onClick={() => addMaterial(lesson)} disabled={busy}>
                                + Thêm link
                              </button>
                            </div>
                          )}
                        </div>
                      )}
                    </li>
                  )
                })}
              </ol>

              {section.lessons.length === 0 && <p className="muted small section-card__empty">Chương chưa có bài học.</p>}

              {editable && (
                <button type="button" className="button button--ghost button--small section-card__add"
                  onClick={() => addLesson(section)} disabled={busy}>
                  + Thêm bài học
                </button>
              )}
            </article>
          ))
        )}
      </section>
    </div>
  )
}
