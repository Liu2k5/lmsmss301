import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { userApi } from '../api/userApi.js'

export default function ProfilePage() {
  const { authenticated, getToken, isAdmin } = useAuth()
  const [profile, setProfile] = useState(null)
  const [universities, setUniversities] = useState([])
  const [form, setForm] = useState({ fullname: '', phone: '', universityId: '' })
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [requests, setRequests] = useState([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [requestBusy, setRequestBusy] = useState(false)

  useEffect(() => {
    if (!authenticated) return
    let active = true
    Promise.all([getToken(), userApi.universities()])
      .then(async ([token, options]) => {
        if (!token) throw new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.')
        const [data, accountRequests] = await Promise.all([
          userApi.me(token),
          userApi.myRequests(token),
        ])
        if (!active) return
        setProfile(data)
        setUniversities(options)
        setRequests(accountRequests)
        setForm({
          fullname: data.fullname ?? '',
          phone: data.phone ?? '',
          universityId: data.universityId == null ? '' : String(data.universityId),
        })
      })
      .catch((requestError) => active && setError(requestError.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [authenticated, getToken])

  async function submitPassword(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      const result = await userApi.changePassword(token, passwordForm)
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setNotice(result.message)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  async function submitAccountRequest(kind) {
    setRequestBusy(true)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      if (kind === 'deletion') {
        await userApi.requestDeletion(token)
      } else {
        await userApi.requestInstructorRole(token)
      }
      setRequests(await userApi.myRequests(token))
      setNotice(kind === 'deletion'
        ? 'Đã gửi yêu cầu xoá tài khoản để admin xem xét.'
        : 'Đã gửi yêu cầu chuyển sang giảng viên để admin xem xét.')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setRequestBusy(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const token = await getToken()
      const updated = await userApi.updateMe(token, {
        fullname: form.fullname,
        phone: form.phone,
        universityId: form.universityId ? Number(form.universityId) : null,
      })
      setProfile(updated)
      setNotice('Thông tin hồ sơ đã được cập nhật.')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  if (!authenticated) {
    return <main className="user-page"><div className="user-alert">Vui lòng <a href="#/login">đăng nhập</a> để xem hồ sơ.</div></main>
  }

  return (
    <main className="user-page">
      <div className="page-title">
        <div><span className="eyebrow">TÀI KHOẢN CỦA BẠN</span><h1>Hồ sơ cá nhân</h1></div>
        {isAdmin && (
          <div className="user-actions">
            <a className="button button--ghost" href="#/admin/users">Quản lý người dùng</a>
            <a className="button button--ghost" href="#/admin/requests">Yêu cầu cần duyệt</a>
          </div>
        )}
      </div>
      {loading ? <div className="user-panel">Đang tải hồ sơ…</div> : (
        <section className="user-panel profile-panel">
          {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
          {notice && <div className="user-alert user-alert--success" role="status">{notice}</div>}
          {profile && (
            <>
              <div className="profile-summary">
                <div className="profile-avatar">{profile.fullname?.charAt(0)?.toUpperCase() || '?'}</div>
                <div><h2>{profile.fullname}</h2><p>{profile.email}</p><span className="role-pill">{profile.role}</span></div>
              </div>
              <form className="user-form" onSubmit={submit}>
                <div className="user-form__grid">
                  <label className="user-field">
                    <span>Tên đăng nhập</span>
                    <input value={profile.username} disabled />
                  </label>
                  <label className="user-field">
                    <span>Email</span>
                    <input value={profile.email} disabled />
                  </label>
                  <label className="user-field">
                    <span>Họ và tên *</span>
                    <input value={form.fullname} onChange={(event) => setForm({ ...form, fullname: event.target.value })} maxLength="128" required />
                  </label>
                  <label className="user-field">
                    <span>Số điện thoại</span>
                    <input value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} maxLength="15" />
                  </label>
                  <label className="user-field user-field--wide">
                    <span>Trường đại học</span>
                    <select value={form.universityId} onChange={(event) => setForm({ ...form, universityId: event.target.value })}>
                      <option value="">Chưa chọn</option>
                      {universities.map((university) => <option key={university.id} value={university.id}>{university.name}</option>)}
                    </select>
                  </label>
                </div>
                <button className="button button--primary" type="submit" disabled={saving}>
                  {saving ? 'Đang lưu…' : 'Lưu thay đổi'}
                </button>
              </form>

              <section className="profile-subsection">
                <span className="eyebrow">BẢO MẬT</span>
                <h2>Đổi mật khẩu</h2>
                <form className="user-form" onSubmit={submitPassword}>
                  <div className="user-form__grid">
                    <label className="user-field">
                      <span>Mật khẩu hiện tại *</span>
                      <input type="password" value={passwordForm.currentPassword}
                        onChange={(event) => setPasswordForm({ ...passwordForm, currentPassword: event.target.value })}
                        autoComplete="current-password" required />
                    </label>
                    <label className="user-field">
                      <span>Mật khẩu mới *</span>
                      <input type="password" value={passwordForm.newPassword}
                        onChange={(event) => setPasswordForm({ ...passwordForm, newPassword: event.target.value })}
                        minLength="8" maxLength="72" autoComplete="new-password" required />
                    </label>
                    <label className="user-field">
                      <span>Xác nhận mật khẩu mới *</span>
                      <input type="password" value={passwordForm.confirmPassword}
                        onChange={(event) => setPasswordForm({ ...passwordForm, confirmPassword: event.target.value })}
                        minLength="8" maxLength="72" autoComplete="new-password" required />
                    </label>
                  </div>
                  <button className="button button--primary" type="submit" disabled={saving}>
                    {saving ? 'Đang đổi…' : 'Đổi mật khẩu'}
                  </button>
                </form>
              </section>

              <section className="profile-subsection">
                <span className="eyebrow">YÊU CẦU TÀI KHOẢN</span>
                <h2>Yêu cầu của tôi</h2>
                <div className="user-actions profile-request-actions">
                  {profile.role === 'STUDENT' && (
                    <button className="button button--ghost" type="button" disabled={requestBusy}
                      onClick={() => submitAccountRequest('instructor')}>
                      Xin làm giảng viên
                    </button>
                  )}
                  <button className="button button--danger-ghost" type="button" disabled={requestBusy || profile.deleted}
                    onClick={() => {
                      if (window.confirm('Gửi yêu cầu xoá tài khoản để admin xem xét?')) {
                        submitAccountRequest('deletion')
                      }
                    }}>
                    Yêu cầu xoá tài khoản
                  </button>
                </div>
                {requests.length ? (
                  <div className="request-list">
                    {requests.map((request) => (
                      <article className="request-list__item" key={request.id}>
                        <div>
                          <strong>{request.type === 'DELETION' ? 'Yêu cầu xoá tài khoản' : 'Yêu cầu làm giảng viên'}</strong>
                          <small>{new Date(request.createdAt).toLocaleString('vi-VN')}</small>
                          {request.adminNote && <p>Phản hồi admin: {request.adminNote}</p>}
                        </div>
                        <span className={`status-pill status-pill--${request.status.toLowerCase()}`}>
                          {request.status === 'PENDING' ? 'Đang chờ' : request.status === 'APPROVED' ? 'Đã duyệt' : 'Từ chối'}
                        </span>
                      </article>
                    ))}
                  </div>
                ) : <p className="muted small profile-empty">Bạn chưa gửi yêu cầu nào.</p>}
              </section>
            </>
          )}
        </section>
      )}
    </main>
  )
}
