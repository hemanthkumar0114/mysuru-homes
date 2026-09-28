import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { fetchLocalities } from '../api/client'
import { ShieldIcon } from './icons'

export default function Footer() {
  const [localities, setLocalities] = useState([])

  useEffect(() => {
    let ignore = false
    fetchLocalities()
      .then((data) => !ignore && setLocalities(data.slice(0, 5)))
      .catch(() => {})
    return () => {
      ignore = true
    }
  }, [])

  return (
    <footer className="site-footer">
      <div className="container footer-inner">
        <div className="footer-grid">
          <div className="footer-brand">
            <span className="brand">
              <span className="brand-mark">M</span>
              Mysuru Homes
            </span>
            <p className="footer-tagline">
              Verified, owner-direct rentals and PGs across Mysuru. No broker
              spam, ever.
            </p>
          </div>

          <div className="footer-col">
            <h3>Localities</h3>
            {localities.length > 0 ? (
              <ul className="footer-links">
                {localities.map((locality) => (
                  <li key={locality.slug}>
                    <Link to={`/localities/${locality.slug}`}>{locality.name}</Link>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted">More areas coming soon.</p>
            )}
          </div>

          <div className="footer-col">
            <h3>Get started</h3>
            <ul className="footer-links">
              <li>
                <Link to="/">Browse listings</Link>
              </li>
              <li>
                <Link to="/post-property">Post a property</Link>
              </li>
              <li>
                <Link to="/register">Create an account</Link>
              </li>
              <li>
                <Link to="/login">Log in</Link>
              </li>
            </ul>
          </div>

          <div className="footer-col">
            <h3>How verification works</h3>
            <p className="footer-verify">
              <ShieldIcon />
              Every listing is physically visited and photographed by our
              field team before it goes live, so what you see is what
              exists.
            </p>
          </div>
        </div>

        <div className="footer-bottom text-muted">
          <span>&copy; {new Date().getFullYear()} Mysuru Homes. Owner-direct, always.</span>
          <span>Made for renters and owners in Mysuru.</span>
        </div>
      </div>
    </footer>
  )
}
