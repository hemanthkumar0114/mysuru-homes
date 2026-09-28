import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PasswordField from '../components/PasswordField'
import { ArchPattern } from '../components/Patterns'
import { CalendarIcon, KeyIcon, ShieldIcon } from '../components/icons'
import { useAuth } from '../context/useAuth'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-split">
        <div className="auth-panel">
          <div className="auth-panel-pattern">
            <ArchPattern id="login-arches" />
          </div>
          <div className="auth-panel-content">
            <span className="brand-mark auth-mark">M</span>
            <h2>Verified rentals, owner-direct.</h2>
            <p>
              Every home on Mysuru Homes is physically checked by our field team before
              it goes live - no broker spam, no surprises.
            </p>
            <ul className="auth-panel-points">
              <li>
                <ShieldIcon />
                Physically verified before every listing goes live
              </li>
              <li>
                <CalendarIcon />
                Book visits and track requests in one place
              </li>
              <li>
                <KeyIcon />
                Talk directly to the owner, start to move-in
              </li>
            </ul>
          </div>
        </div>

        <div className="auth-form-side">
          <div className="card auth-card">
            <span className="brand-mark auth-mark">M</span>
            <h1>Welcome back</h1>
            <p className="text-muted auth-subtitle">
              Log in to enquire about properties and book visits.
            </p>

            <form className="auth-form" onSubmit={handleSubmit}>
              {error && (
                <div className="error-box" role="alert">
                  {error}
                </div>
              )}

              <div className="field">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  autoFocus
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <PasswordField
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />

              <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                {submitting ? 'Logging in…' : 'Log in'}
              </button>
            </form>

            <p className="auth-switch text-muted">
              New to Mysuru Homes? <Link to="/register">Create an account</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
