import { useEffect, useMemo, useState } from 'react'
import Keycloak from 'keycloak-js'
import { AuthContext } from './AuthContext.js'
import { setQuizTokenProvider } from '../api/quizApi.js'
import { setProgressTokenProvider } from '../api/progressApi.js'
import { setEnrollmentTokenProvider } from '../api/enrollmentApi.js'
import { setCourseTokenProvider } from '../api/courseApi.js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'lms',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'lms-web',
})

let initialization

function initializeKeycloak() {
  if (!initialization) {
    initialization = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    })
  }
  return initialization
}

export function AuthProvider({ children }) {
  const [ready, setReady] = useState(false)
  const [authenticated, setAuthenticated] = useState(false)
  const [authError, setAuthError] = useState('')
  // Chỉ cần setter: Keycloak báo trạng thái action (đổi mật khẩu) để JS client xử lý URL.
  const [, setPasswordActionStatus] = useState('')

  useEffect(() => {
    let active = true
    keycloak.onActionUpdate = (status, action) => {
      if (action === 'UPDATE_PASSWORD' && active) {
        setPasswordActionStatus(status)
      }
    }

    initializeKeycloak()
      .then((isAuthenticated) => {
        if (!active) return
        setAuthenticated(isAuthenticated)
        const returnHash = sessionStorage.getItem('postLoginHash')
        if (isAuthenticated && returnHash) {
          sessionStorage.removeItem('postLoginHash')
          if (window.location.hash !== returnHash) window.location.hash = returnHash
        }
        setReady(true)
      })
      .catch((error) => {
        if (!active) return
        console.error('Không khởi tạo được đăng nhập Keycloak', error)
        setAuthError('Không kết nối được Keycloak. Kiểm tra cấu hình và trạng thái Keycloak.')
        setReady(true)
      })

    return () => {
      active = false
    }
  }, [])

  const value = useMemo(() => ({
    ready,
    authenticated,
    authError,
    isAdmin: Boolean(keycloak.tokenParsed?.realm_access?.roles?.some(
      (role) => role.toUpperCase() === 'ADMIN' || role.toUpperCase() === 'ADMINISTRATOR',
    )),
    async login(nextHash = '#/profile') {
      sessionStorage.setItem('postLoginHash', nextHash)
      try {
        await keycloak.login({ redirectUri: `${window.location.origin}${window.location.pathname}` })
      } catch (error) {
        sessionStorage.removeItem('postLoginHash')
        console.error('Không thể bắt đầu đăng nhập Keycloak', error)
        setAuthError('Không thể bắt đầu đăng nhập. Kiểm tra cấu hình Keycloak client.')
      }
    },
    async updatePassword() {
      sessionStorage.setItem('postLoginHash', '#/profile')
      setPasswordActionStatus('')
      try {
        await keycloak.login({
          action: 'UPDATE_PASSWORD',
          prompt: 'login',
          redirectUri: `${window.location.origin}${window.location.pathname}`,
        })
      } catch (error) {
        sessionStorage.removeItem('postLoginHash')
        console.error('Không thể bắt đầu đổi mật khẩu trên Keycloak', error)
        setAuthError('Không thể mở trang đổi mật khẩu của Keycloak.')
      }
    },
    async logout() {
      sessionStorage.removeItem('postLoginHash')
      try {
        await keycloak.logout({ redirectUri: `${window.location.origin}${window.location.pathname}` })
      } catch (error) {
        console.error('Không thể đăng xuất khỏi Keycloak', error)
        setAuthError('Không thể đăng xuất khỏi Keycloak.')
      }
    },
    async getToken() {
      if (!keycloak.authenticated) return null
      await keycloak.updateToken(30)
      return keycloak.token ?? null
    },
    isInstructor: Boolean(keycloak.tokenParsed?.realm_access?.roles?.some(
      (role) => role.toUpperCase() === 'INSTRUCTOR',
    )),
    // Thông tin người đang đăng nhập (sub = studentId/instructorId dùng ở các service).
    user: keycloak.tokenParsed
      ? {
          id: keycloak.tokenParsed.sub,
          username: keycloak.tokenParsed.preferred_username,
          name: keycloak.tokenParsed.name ?? keycloak.tokenParsed.preferred_username,
        }
      : null,
  }), [ready, authenticated, authError])

  // quizApi cần JWT cho mọi request (backend giới hạn quiz cho giảng viên/admin).
  useEffect(() => {
    setQuizTokenProvider(async () => {
      if (!keycloak.authenticated) return null
      try {
        await keycloak.updateToken(30)
      } catch {
        return null
      }
      return keycloak.token ?? null
    })
    return () => setQuizTokenProvider(null)
  }, [ready, authenticated])

  // progressApi (nộp bài quiz - UC-P04) cũng cần JWT khi gọi qua Gateway.
  useEffect(() => {
    setProgressTokenProvider(async () => {
      if (!keycloak.authenticated) return null
      try {
        await keycloak.updateToken(30)
      } catch {
        return null
      }
      return keycloak.token ?? null
    })
    return () => setProgressTokenProvider(null)
  }, [ready, authenticated])

  // enrollmentApi (đăng ký khoá học - UC-E01) cần JWT khi gọi qua Gateway.
  useEffect(() => {
    setEnrollmentTokenProvider(async () => {
      if (!keycloak.authenticated) return null
      try {
        await keycloak.updateToken(30)
      } catch {
        return null
      }
      return keycloak.token ?? null
    })
    return () => setEnrollmentTokenProvider(null)
  }, [ready, authenticated])

  // courseApi (soạn nội dung khoá học - UC-C01…C13) cần JWT cho các thao tác ghi.
  useEffect(() => {
    setCourseTokenProvider(async () => {
      if (!keycloak.authenticated) return null
      try {
        await keycloak.updateToken(30)
      } catch {
        return null
      }
      return keycloak.token ?? null
    })
    return () => setCourseTokenProvider(null)
  }, [ready, authenticated])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
