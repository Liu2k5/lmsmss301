import { useEffect, useMemo, useState } from 'react'
import Keycloak from 'keycloak-js'
import { AuthContext } from './AuthContext.js'

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

  useEffect(() => {
    let active = true
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
    isAdmin: Boolean(keycloak.tokenParsed?.realm_access?.roles?.includes('ADMINISTRATOR')),
    async login() {
      sessionStorage.setItem('postLoginHash', '#/profile')
      try {
        await keycloak.login({ redirectUri: `${window.location.origin}${window.location.pathname}` })
      } catch (error) {
        sessionStorage.removeItem('postLoginHash')
        console.error('Không thể bắt đầu đăng nhập Keycloak', error)
        setAuthError('Không thể bắt đầu đăng nhập. Kiểm tra cấu hình Keycloak client.')
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
  }), [ready, authenticated, authError])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
