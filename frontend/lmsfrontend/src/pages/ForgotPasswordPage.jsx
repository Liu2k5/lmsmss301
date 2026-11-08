import { useState } from 'react'
import { userApi } from '../api/userApi.js'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [codeRequested, setCodeRequested] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function requestCode(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = await userApi.requestPasswordReset(email)
      setCodeRequested(true)
      setNotice(result.message)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  async function reset(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = await userApi.resetPassword({ email, code, newPassword, confirmPassword })
      setNotice(result.message)
      setCodeRequested(false)
      setCode('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="user-page auth-page">
      <section className="auth-card">
        <span className="eyebrow">KHÔI PHỤC TÀI KHOẢN</span>
        <h1>Đặt lại mật khẩu</h1>
        <p className="muted">Nhận mã xác nhận qua email. Mã có hiệu lực trong 10 phút.</p>
        {error && <div className="user-alert user-alert--error" role="alert">{error}</div>}
        {notice && <div className="user-alert user-alert--success" role="status">{notice}</div>}
        <form className="user-form" onSubmit={codeRequested ? reset : requestCode}>
          <label className="user-field">
            <span>Email tài khoản *</span>
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoComplete="email" />
          </label>
          {codeRequested && (
            <>
              <label className="user-field">
                <span>Mã xác nhận 6 chữ số *</span>
                <input value={code} onChange={(event) => setCode(event.target.value)} inputMode="numeric" pattern="[0-9]{6}" maxLength="6" required />
              </label>
              <label className="user-field">
                <span>Mật khẩu mới *</span>
                <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength="8" maxLength="72" required autoComplete="new-password" />
              </label>
              <label className="user-field">
                <span>Nhập lại mật khẩu mới *</span>
                <input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength="8" maxLength="72" required autoComplete="new-password" />
              </label>
            </>
          )}
          <button className="button button--primary button--full" type="submit" disabled={busy}>
            {busy ? 'Đang xử lý…' : codeRequested ? 'Đặt lại mật khẩu' : 'Gửi mã xác nhận'}
          </button>
        </form>
        <p className="auth-card__foot"><a href="#/login">Quay lại đăng nhập</a></p>
      </section>
    </main>
  )
}
