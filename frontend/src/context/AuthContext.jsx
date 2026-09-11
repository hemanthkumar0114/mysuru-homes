import { createContext, useContext, useEffect, useState } from 'react'
import { getToken, loginUser, registerUser, setToken } from '../api/client'

const USER_KEY = 'mysuruhomes_user'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem(USER_KEY)
      return saved ? JSON.parse(saved) : null
    } catch {
      return null
    }
  })

  // Keep localStorage in sync whenever the user changes.
  useEffect(() => {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }, [user])

  async function login(email, password) {
    const result = await loginUser({ email, password })
    setToken(result.token)
    setUser(result.user)
    return result.user
  }

  async function register(name, email, password, role) {
    const result = await registerUser({ name, email, password, role })
    setToken(result.token)
    setUser(result.user)
    return result.user
  }

  function logout() {
    setToken(null)
    setUser(null)
  }

  const isLoggedIn = Boolean(user && getToken())

  return (
    <AuthContext.Provider value={{ user, isLoggedIn, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return ctx
}
