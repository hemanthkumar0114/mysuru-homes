import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, isLoggedIn, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container row-between">
        <Link to="/" className="brand">
          Mysuru Homes
        </Link>

        <nav>
          <Link to="/">Browse</Link>

          {isLoggedIn && user.role === 'OWNER' && (
            <>
              <Link to="/post-property">Post a property</Link>
              <Link to="/my-listings">My listings</Link>
            </>
          )}

          {isLoggedIn && user.role === 'ADMIN' && (
            <Link to="/admin">Admin review</Link>
          )}

          {isLoggedIn ? (
            <>
              <span className="user-badge">
                {user.name} ({user.role.toLowerCase()})
              </span>
              <button type="button" onClick={handleLogout}>
                Log out
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Log in</Link>
              <Link to="/register">Sign up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
