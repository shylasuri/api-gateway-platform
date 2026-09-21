import { createContext, useContext, useState, ReactNode } from 'react'
import { AuthResponse } from '../types'

interface AuthUser {
  userId: string
  email: string
  fullName: string
  role: 'CONSUMER' | 'ADMIN'
}

interface AuthContextValue {
  user: AuthUser | null
  login: (response: AuthResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem('gw_user')
    return raw ? JSON.parse(raw) : null
  })

  function login(response: AuthResponse) {
    const authUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    }
    localStorage.setItem('gw_token', response.token)
    localStorage.setItem('gw_user', JSON.stringify(authUser))
    setUser(authUser)
  }

  function logout() {
    localStorage.removeItem('gw_token')
    localStorage.removeItem('gw_user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
