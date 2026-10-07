import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CameraIcon, CheckIcon, NoBrokerIcon, ShieldIcon } from '../components/icons'
import PasswordField from '../components/PasswordField'
import { ArchPattern } from '../components/Patterns'
import { useAuth } from '../context/useAuth'

const ROLES = [
  { value: 'TENANT', title: 'Tenant', description: 'I want to rent a home or PG' },
  { value: 'OWNER', title: 'Owner', description: 'I want to list my property' },
]

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('TENANT')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await register(name, email, password, role)
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
            <ArchPattern id="register-arches" />
          </div>
          <div className="auth-panel-content">
            <span className="brand-mark auth-mark">M</span>
            <h2>Join Mysuru Homes</h2>
            <p>
              Whether you&apos;re looking for a home or listing one, everything here is
              owner-direct and field-verified.
            </p>
            <ul className="auth-panel-points">
              <li>
                <ShieldIcon />
                Every listing is physically checked before it goes live
              </li>
              <li>
                <CameraIcon />
                Real photos from the property, not stock images
              </li>
              <li>
                <NoBrokerIcon />
                No broker fees for tenants or owners
              </li>
            </ul>
          </div>
        </div>

        <div className="auth-form-side">
          <div className="card auth-card">
            <span className="brand-mark auth-mark">M</span>
            <h1>Create your account</h1>
            <p className="text-muted auth-subtitle">
              Find verified rentals in Mysuru, or list your own property.
            </p>

            <form className="auth-form" onSubmit={handleSubmit}>
          {error && (
            <div className="error-box" role="alert">
              {error}
            </div>
          )}

          <fieldset className="choice-picker">
            <legend>I am a…</legend>
            <div className="choice-options">
              {ROLES.map((option) => (
                <label
                  key={option.value}
                  className={role === option.value ? 'choice-option selected' : 'choice-option'}
                >
                  <input
                    type="radio"
                    name="role"
                    value={option.value}
                    checked={role === option.value}
                    onChange={() => setRole(option.value)}
                  />
                  <span className="choice-title">
                    {option.title}
                    {role === option.value && <CheckIcon />}
                  </span>
                  <span className="choice-desc">{option.description}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <div className="field">
            <label htmlFor="name">Full name</label>
            <input
              id="name"
              autoComplete="name"
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <PasswordField
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            minLength={8}
            hint="8 to 72 characters."
          />

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
            </form>

            <p className="auth-switch text-muted">
              Already have an account? <Link to="/login">Log in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
