import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

/**
 * Wrap a page element with this to require login (and optionally a role).
 * Usage: <ProtectedRoute role="OWNER"><PostProperty /></ProtectedRoute>
 */
export default function ProtectedRoute({ role, children }) {
  const { user, isLoggedIn } = useAuth()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />
  }

  if (role && user.role !== role) {
    return (
      <div className="container page">
        <div className="error-box">
          This page is only available to {role.toLowerCase()} accounts.
        </div>
      </div>
    )
  }

  return children
}
