import { useEffect, useMemo, useState } from 'react'
import Keycloak from 'keycloak-js'
import { AuthContext } from './AuthContext.js'
import { setQuizTokenProvider } from '../api/quizApi.js'
import { setProgressTokenProvider } from '../api/progressApi.js'
import { setEnrollmentTokenProvider } from '../api/enrollmentApi.js'
import { setReviewTokenProvider } from '../api/reviewApi.js'
import { setCourseTokenProvider } from '../api/courseApi.js'

// Giữ MỘT instance Keycloak cho cả trang, kể cả khi Vite hot-reload chạy lại module này.
// Nếu tạo mới mỗi lần hot-reload, instance mới chưa init → authenticated = undefined: mọi API báo
// "Phiên đăng nhập hết hạn" và nút Đăng xuất lỗi ngầm (keycloak.logout ném TypeError).
const shared = (globalThis.__lmsAuth ??= {})
const keycloak = (shared.keycloak ??= new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'lms',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'lms-web',
}))

/**
 * Token dùng cho các api module (quiz/progress/enrollment/course).
 * Đăng ký ở cấp module vì effect của component con chạy TRƯỚC effect của AuthProvider:
 * nếu đăng ký trong useEffect, lần tải trang đầu tiên các request sẽ đi không token (401).
 */
async function getAccessToken() {
  if (!keycloak.authenticated) return null
  try {
    await keycloak.updateToken(30)
  } catch {
    return null
  }
  return keycloak.token ?? null
}

setQuizTokenProvider(getAccessToken)
setProgressTokenProvider(getAccessToken)
setEnrollmentTokenProvider(getAccessToken)
setReviewTokenProvider(getAccessToken)
setCourseTokenProvider(getAccessToken)

function initializeKeycloak() {
  if (!shared.initialization) {
    shared.initialization = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    })
  }
  return shared.initialization
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

    // Phiên Keycloak hết hạn (làm mới token bị từ chối) → keycloak-js tự xoá token và gọi onAuthLogout.
    // Đồng bộ lại state để giao diện chuyển về trạng thái "chưa đăng nhập" thay vì kẹt nửa vời.
    keycloak.onAuthLogout = () => {
      if (active) setAuthenticated(false)
    }

    // Giữ phiên sống khi tab đang mở: làm mới token trước khi hết hạn, kể cả khi người dùng
    // chỉ đọc trang mà không gọi API (nếu không, phiên Keycloak sẽ hết hạn do "idle").
    const refreshTimer = window.setInterval(() => {
      if (!keycloak.authenticated) return
      keycloak.updateToken(60).catch(() => {
        // Lỗi mạng tạm thời: thử lại ở lần sau. Lỗi 400 (phiên hết hạn) đã được xử lý qua onAuthLogout.
      })
    }, 60_000)

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
      window.clearInterval(refreshTimer)
      keycloak.onAuthLogout = undefined
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
      // Phiên đã hết hạn (không còn token): không cần qua Keycloak, chỉ cần về trạng thái chưa đăng nhập.
      if (!keycloak.authenticated) {
        setAuthenticated(false)
        window.location.hash = '#/'
        return
      }
      try {
        await keycloak.logout({ redirectUri: `${window.location.origin}${window.location.pathname}` })
      } catch (error) {
        console.error('Không thể đăng xuất khỏi Keycloak', error)
        setAuthError('Không thể đăng xuất khỏi Keycloak.')
      }
    },
    getToken: getAccessToken,
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

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
