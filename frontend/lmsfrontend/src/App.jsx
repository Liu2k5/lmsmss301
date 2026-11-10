import { useEffect, useState } from 'react'
import './App.css'
import './courses.css'
import './user.css'
import CoursePage from './pages/CoursePage.jsx'
import QuizPage from './pages/QuizPage.jsx'
import QuizAttemptPage from './pages/QuizAttemptPage.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import AdminRequestsPage from './pages/AdminRequestsPage.jsx'
import AdminCategoriesPage from './pages/AdminCategoriesPage.jsx'
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx'
import { useAuth } from './auth/AuthContext.js'

/**
 * Điều hướng đơn giản bằng hash:
 * #/courses, #/courses/:id, #/quizzes, #/quizzes?quizId=<uuid>, #/quizzes?lessonId=<uuid>
 * #/quiz-attempt?quizId=<uuid>&courseId=<uuid>  — học viên làm bài (UC-P04)
 * #/admin/users, #/admin/requests, #/admin/categories — trang quản trị (chỉ Admin)
 */
function parseHash(hash) {
  const raw = hash.replace(/^#\/?/, '')
  const [pathPart, queryPart = ''] = raw.split('?')
  const parts = pathPart.split('/').filter(Boolean)
  const params = new URLSearchParams(queryPart)
  const uuid = (value) =>
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value ?? '') ? value : null

  if (parts[0] === 'quizzes') {
    return {
      page: 'quizzes',
      lessonId: uuid(params.get('lessonId')),
      quizId: uuid(params.get('quizId')),
    }
  }
  if (parts[0] === 'quiz-attempt') {
    return {
      page: 'quiz-attempt',
      quizId: uuid(params.get('quizId')),
      quizCourseId: uuid(params.get('courseId')),
    }
  }
  if (['login', 'register', 'profile', 'forgot-password'].includes(parts[0])) {
    return { page: parts[0] }
  }
  if (parts[0] === 'admin' && parts[1] === 'users') {
    return { page: 'admin-users' }
  }
  if (parts[0] === 'admin' && parts[1] === 'requests') {
    return { page: 'admin-requests' }
  }
  if (parts[0] === 'admin' && parts[1] === 'categories') {
    return { page: 'admin-categories' }
  }
  if (!parts.length) return { page: 'home' }
  const id = uuid(parts[1])
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
                <>
                  <a href="#/admin/users" className={`top-nav__link${route.page === 'admin-users' ? ' is-active' : ''}`}
                    aria-current={route.page === 'admin-users' ? 'page' : undefined}>
                    Người dùng
                  </a>
                  <a href="#/admin/requests" className={`top-nav__link${route.page === 'admin-requests' ? ' is-active' : ''}`}
                    aria-current={route.page === 'admin-requests' ? 'page' : undefined}>
                    Yêu cầu
                  </a>
                  <a href="#/admin/categories" className={`top-nav__link${route.page === 'admin-categories' ? ' is-active' : ''}`}
                    aria-current={route.page === 'admin-categories' ? 'page' : undefined}>
                    Danh mục
                  </a>
                </>
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
          route.page === 'forgot-password' ? <ForgotPasswordPage /> :
          route.page === 'profile' ? <ProfilePage /> :
            route.page === 'admin-users' ? <AdminUsersPage /> :
              route.page === 'admin-requests' ? <AdminRequestsPage /> :
              route.page === 'admin-categories' ? <AdminCategoriesPage /> :
      route.page === 'quiz-attempt' ? (
        <QuizAttemptPage quizId={route.quizId} courseId={route.quizCourseId} />
      ) :
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
