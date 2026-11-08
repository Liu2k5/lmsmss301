import { useAuth } from '../auth/AuthContext.js'

export default function HomePage() {
  const { authenticated, isAdmin } = useAuth()

  return (
    <main className="user-page home-page">
      <section className="home-hero">
        <div className="home-hero__copy">
          <span className="eyebrow">LEARNHUB · LEARN AT YOUR PACE</span>
          <h1>Học điều mới,<br />mở lối tương lai.</h1>
          <p>Không gian học tập trực tuyến để bạn khám phá khoá học, theo dõi tiến độ và phát triển kỹ năng mỗi ngày.</p>
          <div className="user-actions">
            <a className="button button--primary" href="#/courses">Khám phá khoá học</a>
            <a className="button button--ghost" href={authenticated ? '#/profile' : '#/register'}>
              {authenticated ? 'Hồ sơ của tôi' : 'Tạo tài khoản'}
            </a>
          </div>
        </div>
        <div className="home-hero__art" aria-hidden="true">
          <div className="hero-orbit hero-orbit--one" />
          <div className="hero-orbit hero-orbit--two" />
          <div className="hero-book">✦</div>
          <div className="hero-note hero-note--top">HỌC MỖI NGÀY</div>
          <div className="hero-note hero-note--bottom">Tiến bộ từng chút một</div>
        </div>
      </section>

      <section className="home-section">
        <div className="section-heading">
          <div><span className="eyebrow">BẮT ĐẦU NGAY</span><h2>Mọi thứ bạn cần để học tốt hơn</h2></div>
          <a className="text-link" href="#/courses">Xem khoá học <span aria-hidden="true">→</span></a>
        </div>
        <div className="home-feature-grid">
          <a className="home-feature" href="#/courses">
            <span className="feature-icon feature-icon--blue">▧</span>
            <h3>Khoá học đa dạng</h3>
            <p>Tìm nội dung phù hợp và học theo nhịp độ của riêng bạn.</p>
          </a>
          <a className="home-feature" href="#/quizzes">
            <span className="feature-icon feature-icon--orange">✓</span>
            <h3>Luyện tập cùng quiz</h3>
            <p>Củng cố kiến thức với những bài kiểm tra ngắn sau bài học.</p>
          </a>
          <a className="home-feature" href={authenticated ? '#/profile' : '#/login'}>
            <span className="feature-icon feature-icon--green">◎</span>
            <h3>Theo dõi hành trình</h3>
            <p>Cập nhật hồ sơ và quản lý thông tin tài khoản của bạn.</p>
          </a>
          {isAdmin && (
            <a className="home-feature" href="#/admin/users">
              <span className="feature-icon feature-icon--violet">♙</span>
              <h3>Quản lý người dùng</h3>
              <p>Quản lý vai trò, hồ sơ và trạng thái tài khoản.</p>
            </a>
          )}
        </div>
      </section>
    </main>
  )
}
