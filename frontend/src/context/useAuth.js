import { createContext, useContext } from 'react'

// The context object and the hook that reads it live here, not in
// AuthContext.jsx: a file that exports components must export only
// components, or React's fast refresh can't hot-reload it.
export const AuthContext = createContext(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return ctx
}
