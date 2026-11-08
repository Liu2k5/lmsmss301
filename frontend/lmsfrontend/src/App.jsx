import { useEffect, useState } from 'react'
import './App.css'
import './courses.css'
import './user.css'
import CoursePage from './pages/CoursePage.jsx'
import QuizPage from './pages/QuizPage.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import { useAuth } from './auth/AuthContext.js'

/**
 * Điều hướng đơn giản bằng hash:
 * #/courses, #/courses/:id, #/quizzes, #/quizzes?quizId=5, #/quizzes?lessonId=7
 */
function parseHash(hash) {
  const raw = hash.replace(/^#\/?/, '')
  const [pathPart, queryPart = ''] = raw.split('?')
  const parts = pathPart.split('/').filter(Boolean)
  const params = new URLSearchParams(queryPart)
  const positive = (value) => {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null
  }

  if (parts[0] === 'quizzes') {
    return {
      page: 'quizzes',
      lessonId: positive(params.get('lessonId')),
      quizId: positive(params.get('quizId')),
    }
  }
  if (['login', 'register', 'profile'].includes(parts[0])) {
    return { page: parts[0] }
  }
  if (parts[0] === 'admin' && parts[1] === 'users') {
    return { page: 'admin-users' }
  }
  if (!parts.length) return { page: 'home' }
  const id = positive(parts[1])
  return { page: 'courses', courseId: parts[0] === 'courses' ? id : null }
}

function navigate(path) {
  window.location.hash = path
}

export default function App() {
  const { authenticated, isAdmin, logout } = useAuth()
  const [route, setRoute] = useState(() => parseHash(window.location.hash))

  useEffect(() => {
    function handleHashChange() {
      setRoute(parseHash(window.location.hash))
      window.scrollTo({ top: 0 })
    }
    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  return (
    <>
      <nav className="top-nav" aria-label="Điều hướng chính">
        <div className="top-nav__inner">
          <a href="#/" className="top-nav__brand">LearnHub</a>
          <a href="#/" className={`top-nav__link${route.page === 'home' ? ' is-active' : ''}`}
            aria-current={route.page === 'home' ? 'page' : undefined}>
            Trang chủ
          </a>
          <a href="#/courses" className={`top-nav__link${route.page === 'courses' ? ' is-active' : ''}`}
            aria-current={route.page === 'courses' ? 'page' : undefined}>
            Khoá học
          </a>
          <a href="#/quizzes" className={`top-nav__link${route.page === 'quizzes' ? ' is-active' : ''}`}
            aria-current={route.page === 'quizzes' ? 'page' : undefined}>
            Quiz
          </a>
          {authenticated ? (
            <>
              <a href="#/profile" className={`top-nav__link${route.page === 'profile' ? ' is-active' : ''}`}
                aria-current={route.page === 'profile' ? 'page' : undefined}>
                Hồ sơ
              </a>
              {isAdmin && (
                <a href="#/admin/users" className={`top-nav__link${route.page === 'admin-users' ? ' is-active' : ''}`}
                  aria-current={route.page === 'admin-users' ? 'page' : undefined}>
                  Quản lý người dùng
                </a>
              )}
              <button className="top-nav__auth" type="button" onClick={logout}>Đăng xuất</button>
            </>
          ) : (
            <>
              <a href="#/login" className={`top-nav__link${route.page === 'login' ? ' is-active' : ''}`}>Đăng nhập</a>
              <a href="#/register" className="button button--primary button--small">Đăng ký</a>
            </>
          )}
        </div>
      </nav>

      {route.page === 'home' ? <HomePage /> :
        route.page === 'login' ? <LoginPage /> :
          route.page === 'register' ? <RegisterPage /> :
            route.page === 'profile' ? <ProfilePage /> :
              route.page === 'admin-users' ? <AdminUsersPage /> :
      route.page === 'quizzes' ? (
        <QuizPage initialLessonId={route.lessonId} initialQuizId={route.quizId} />
      ) : (
        <CoursePage
          courseId={route.courseId}
          onOpenCourse={(id) => navigate(`/courses/${id}`)}
          onBack={() => navigate('/courses')}
        />
      )}
    </>
  )
}
