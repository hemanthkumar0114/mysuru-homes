import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

// NavLink is Link plus "am I the current page?". We use it to highlight the
// active item; `end` on "/" stops Browse being highlighted on every page.
function navClass({ isActive }) {
  return isActive ? 'nav-link active' : 'nav-link'
}

export default function Navbar() {
  const { user, isLoggedIn, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand">
          <span className="brand-mark">M</span>
          Mysuru Homes
        </Link>

        <nav>
          <NavLink to="/" end className={navClass}>
            Browse
          </NavLink>

          {isLoggedIn && user.role === 'OWNER' && (
            <>
              <NavLink to="/post-property" className={navClass}>
                Post a property
              </NavLink>
              <NavLink to="/my-listings" className={navClass}>
                My listings
              </NavLink>
            </>
          )}

          {isLoggedIn && user.role === 'ADMIN' && (
            <NavLink to="/admin" className={navClass}>
              Admin review
            </NavLink>
          )}

          {isLoggedIn ? (
            <>
              <span className="user-chip">
                {user.name}
                <span className="role-pill">{user.role.toLowerCase()}</span>
              </span>
              <button type="button" className="btn btn-sm" onClick={handleLogout}>
                Log out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={navClass}>
                Log in
              </NavLink>
              <Link to="/register" className="btn btn-primary btn-sm">
                Sign up
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
