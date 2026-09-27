import { useState } from 'react'

// A password input with a Show / Hide button. Shared by Login and Register.
export default function PasswordField({
  id = 'password',
  label = 'Password',
  value,
  onChange,
  autoComplete,
  minLength,
  hint,
}) {
  const [visible, setVisible] = useState(false)

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="password-wrap">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          minLength={minLength}
          required
        />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
      {hint && <span className="field-hint text-muted">{hint}</span>}
    </div>
  )
}
