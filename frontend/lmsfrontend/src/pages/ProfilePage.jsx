import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.js'
import { userApi } from '../api/userApi.js'

export default function ProfilePage() {
  const { authenticated, getToken, isAdmin } = useAuth()
  const [profile, setProfile] = useState(null)
  const [universities, setUniversities] = useState([])
  const [form, setForm] = useState({ fullname: '', phone: '', universityId: '' })
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!authenticated) return
    let active = true
    Promise.all([getToken(), userApi.universities()])
      .then(async ([token, options]) => {
        if (!token) throw new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.')
        const data = await userApi.me(token)
        if (!active) return
        setProfile(data)
        setUniversities(options)
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
        {isAdmin && <a className="button button--ghost" href="#/admin/users">Quản lý người dùng</a>}
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
            </>
          )}
        </section>
      )}
    </main>
  )
}
