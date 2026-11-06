import { useEffect, useState } from 'react'
import './App.css'
import './courses.css'
import CoursePage from './pages/CoursePage.jsx'
import QuizPage from './pages/QuizPage.jsx'

/** Điều hướng đơn giản bằng hash: #/courses, #/courses/:id, #/quizzes. */
function parseHash(hash) {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  if (parts[0] === 'quizzes') {
    return { page: 'quizzes' }
  }
  const id = Number(parts[1])
  return { page: 'courses', courseId: parts[0] === 'courses' && Number.isInteger(id) && id > 0 ? id : null }
}

function navigate(path) {
  window.location.hash = path
}

export default function App() {
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
          <span className="top-nav__brand">LearnHub</span>
          <a href="#/courses" className={`top-nav__link${route.page === 'courses' ? ' is-active' : ''}`}
            aria-current={route.page === 'courses' ? 'page' : undefined}>
            Khoá học
          </a>
          <a href="#/quizzes" className={`top-nav__link${route.page === 'quizzes' ? ' is-active' : ''}`}
            aria-current={route.page === 'quizzes' ? 'page' : undefined}>
            Quiz
          </a>
        </div>
      </nav>

      {route.page === 'quizzes' ? (
        <QuizPage />
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
