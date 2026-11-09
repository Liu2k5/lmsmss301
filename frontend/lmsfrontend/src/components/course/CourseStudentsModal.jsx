import { useCallback, useEffect, useState } from 'react'
import { enrollmentApi } from '../../api/enrollmentApi.js'
import { formatDate } from './courseMeta.js'

export default function CourseStudentsModal({ courseId, courseTitle, isAdmin, onClose, notify }) {
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [showGrantForm, setShowGrantForm] = useState(false)
  const [grantStudentId, setGrantStudentId] = useState('')
  const [granting, setGranting] = useState(false)

  const loadStudents = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await enrollmentApi.getCourseStudents(courseId)
      setStudents(list)
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách học viên.')
    } finally {
      setLoading(false)
    }
  }, [courseId])

  useEffect(() => {
    loadStudents()
  }, [loadStudents])

  async function handleRevoke(studentId) {
    if (!window.confirm(`Bạn có chắc chắn muốn thu hồi quyền truy cập khoá học của học viên #${studentId.slice(0, 8)}?`)) {
      return
    }
    setBusyId(studentId)
    try {
      await enrollmentApi.revoke(studentId, courseId)
      notify('success', `Đã thu hồi quyền truy cập của học viên #${studentId.slice(0, 8)}.`)
      await loadStudents()
    } catch (err) {
      notify('error', err.message || 'Không thể thu hồi quyền.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleGrant(event) {
    event.preventDefault()
    const targetId = grantStudentId.trim()
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(targetId)) {
      notify('error', 'UUID học viên không hợp lệ (định dạng 8-4-4-4-12 ký tự hex).')
      return
    }
    setGranting(true)
    try {
      await enrollmentApi.grant(targetId, courseId)
      notify('success', `Đã cấp quyền tham gia khoá học cho học viên #${targetId.slice(0, 8)}.`)
      setGrantStudentId('')
      setShowGrantForm(false)
      await loadStudents()
    } catch (err) {
      notify('error', err.message || 'Không thể cấp quyền tham gia.')
    } finally {
      setGranting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal modal--medium" role="dialog" aria-modal="true" aria-labelledby="students-modal-title">
        <header className="modal__header">
          <div>
            <h2 id="students-modal-title">Học viên đã đăng ký ({students.length})</h2>
            <p className="muted small">{courseTitle}</p>
          </div>
          <button type="button" className="icon-button icon-button--neutral" onClick={onClose} aria-label="Đóng">
            ✕
          </button>
        </header>

        <div className="modal__body">
          {isAdmin && (
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="button button--small button--primary"
                onClick={() => setShowGrantForm((prev) => !prev)}
              >
                {showGrantForm ? 'Đóng form cấp quyền' : '+ Cấp quyền học viên (Admin)'}
              </button>
            </div>
          )}

          {showGrantForm && (
            <form onSubmit={handleGrant} style={{ background: 'var(--color-bg-subtle, #f8f9fa)', padding: 16, borderRadius: 8, marginBottom: 20 }}>
              <h4 style={{ margin: '0 0 8px' }}>Cấp quyền truy cập khoá học (UC-E06)</h4>
              <p className="muted small" style={{ margin: '0 0 12px' }}>
                Nhập UUID học viên cần cấp quyền vào khoá học này. Học viên sẽ được kích hoạt tiến độ ngay lập tức.
              </p>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  className="input"
                  style={{ flex: 1 }}
                  placeholder="UUID học viên (ví dụ: 11111111-1111-1111-1111-111111111111)"
                  value={grantStudentId}
                  onChange={(e) => setGrantStudentId(e.target.value)}
                  disabled={granting}
                  required
                />
                <button type="submit" className="button button--primary button--small" disabled={granting || !grantStudentId.trim()}>
                  {granting ? 'Đang cấp…' : 'Xác nhận cấp quyền'}
                </button>
              </div>
            </form>
          )}

          {loading ? (
            <div className="state-card state-card--compact">
              <span className="spinner" aria-hidden="true" />
              <p>Đang tải danh sách học viên…</p>
            </div>
          ) : error ? (
            <div className="alert alert--error" role="alert">
              <strong>Lỗi:</strong> {error}
            </div>
          ) : students.length === 0 ? (
            <div className="state-card state-card--compact">
              <p>Chưa có học viên nào đang theo học khoá học này.</p>
            </div>
          ) : (
            <div style={{ maxHeight: 360, overflowY: 'auto' }}>
              <table className="user-table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--color-border)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 12px' }}>Học viên</th>
                    <th style={{ padding: '8px 12px' }}>Ngày đăng ký</th>
                    <th style={{ padding: '8px 12px' }}>Trạng thái</th>
                    {isAdmin && <th style={{ padding: '8px 12px', textAlign: 'right' }}>Thao tác</th>}
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => (
                    <tr key={student.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <td style={{ padding: '8px 12px' }}>
                        <code>#{student.studentId.slice(0, 8)}</code>
                        <span className="muted small" style={{ display: 'block' }}>{student.studentId}</span>
                      </td>
                      <td style={{ padding: '8px 12px' }}>{formatDate(student.enrolledAt)}</td>
                      <td style={{ padding: '8px 12px' }}>
                        <span className="status-badge status-badge--success">Đang học</span>
                      </td>
                      {isAdmin && (
                        <td style={{ padding: '8px 12px', textAlign: 'right' }}>
                          <button
                            type="button"
                            className="button button--small button--danger-ghost"
                            onClick={() => handleRevoke(student.studentId)}
                            disabled={busyId === student.studentId}
                            title="Thu hồi quyền truy cập (UC-E07)"
                          >
                            {busyId === student.studentId ? 'Đang thu hồi…' : 'Thu hồi quyền'}
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
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
