import { useAuth } from '../auth/AuthContext.js'

export default function LoginPage() {
  const { authenticated, authError, login } = useAuth()

  return (
    <main className="user-page auth-page">
      <section className="auth-card">
        <span className="eyebrow">CHÀO MỪNG TRỞ LẠI</span>
        <h1>Đăng nhập LearnHub</h1>
        <p className="muted">Tài khoản được xác thực an toàn qua Keycloak.</p>
        {authError && <div className="user-alert user-alert--error" role="alert">{authError}</div>}
        {authenticated ? (
          <a className="button button--primary button--full" href="#/profile">Mở hồ sơ của tôi</a>
        ) : (
          <button className="button button--primary button--full" type="button" onClick={login}>
            Tiếp tục với Keycloak
          </button>
        )}
        {!authenticated && <p className="auth-card__foot"><a href="#/forgot-password">Quên mật khẩu?</a></p>}
        <p className="auth-card__foot">Chưa có tài khoản? <a href="#/register">Đăng ký miễn phí</a></p>
      </section>
    </main>
  )
}
