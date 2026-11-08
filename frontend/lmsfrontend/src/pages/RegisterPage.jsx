import { useEffect, useState } from 'react'
import { userApi } from '../api/userApi.js'

const initialForm = {
  username: '',
  fullname: '',
  email: '',
  password: '',
  phone: '',
  role: 'STUDENT',
  universityId: '',
}

export default function RegisterPage() {
  const [form, setForm] = useState(initialForm)
  const [universities, setUniversities] = useState([])
  const [error, setError] = useState('')
  const [details, setDetails] = useState([])
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    userApi.universities().then(setUniversities).catch((requestError) => setError(requestError.message))
  }, [])

  function change(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setDetails([])
    setSubmitting(true)
    try {
      await userApi.register({
        ...form,
        universityId: form.universityId ? Number(form.universityId) : null,
      })
      setSuccess(true)
    } catch (requestError) {
      setError(requestError.message)
      setDetails(requestError.details)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="user-page auth-page">
      <section className="auth-card auth-card--wide">
        <span className="eyebrow">THAM GIA LEARNHUB</span>
        <h1>Tạo tài khoản học tập</h1>
        <p className="muted">Tài khoản sẽ được tạo đồng thời trên hệ thống và Keycloak.</p>
        {success ? (
          <div className="user-alert user-alert--success" role="status">
            Đăng ký thành công. <a href="#/login">Đăng nhập bằng Keycloak</a> để tiếp tục.
          </div>
        ) : (
          <form className="user-form" onSubmit={submit}>
            {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
            <ValidationDetails details={details} />
            <div className="user-form__grid">
              <label className="user-field">
                <span>Tên đăng nhập *</span>
                <input name="username" value={form.username} onChange={change} maxLength="20" required autoComplete="username" />
              </label>
              <label className="user-field">
                <span>Họ và tên *</span>
                <input name="fullname" value={form.fullname} onChange={change} maxLength="128" required autoComplete="name" />
              </label>
              <label className="user-field">
                <span>Email *</span>
                <input type="email" name="email" value={form.email} onChange={change} maxLength="128" required autoComplete="email" />
              </label>
              <label className="user-field">
                <span>Mật khẩu *</span>
                <input type="password" name="password" value={form.password} onChange={change} minLength="8" maxLength="72" required autoComplete="new-password" />
                <small>Tối thiểu 8 ký tự.</small>
              </label>
              <label className="user-field">
                <span>Số điện thoại</span>
                <input name="phone" value={form.phone} onChange={change} maxLength="15" autoComplete="tel" />
              </label>
              <label className="user-field">
                <span>Vai trò *</span>
                <select name="role" value={form.role} onChange={change}>
                  <option value="STUDENT">Học viên</option>
                  <option value="INSTRUCTOR">Giảng viên</option>
                </select>
              </label>
              <label className="user-field user-field--wide">
                <span>Trường đại học</span>
                <select name="universityId" value={form.universityId} onChange={change}>
                  <option value="">Chưa chọn</option>
                  {universities.map((university) => (
                    <option key={university.id} value={university.id}>{university.name}</option>
                  ))}
                </select>
              </label>
            </div>
            <button className="button button--primary button--full" type="submit" disabled={submitting}>
              {submitting ? 'Đang tạo tài khoản…' : 'Đăng ký'}
            </button>
          </form>
        )}
        <p className="auth-card__foot">Đã có tài khoản? <a href="#/login">Đăng nhập</a></p>
      </section>
    </main>
  )
}

function ValidationDetails({ details }) {
  if (!details?.length) return null
  return (
    <ul className="validation-list">
      {details.map((item, index) => <li key={`${item.field}-${index}`}>{item.field}: {item.message}</li>)}
    </ul>
  )
}
