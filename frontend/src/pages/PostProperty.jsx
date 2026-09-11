import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createListing } from '../api/client'

const initialForm = {
  type: 'RENT',
  title: '',
  addressLine: '',
  locality: '',
  lat: '',
  lng: '',
  rentAmount: '',
  bedrooms: '',
  bathrooms: '',
}

export default function PostProperty() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await createListing({
        type: form.type,
        title: form.title,
        addressLine: form.addressLine,
        locality: form.locality,
        lat: Number(form.lat),
        lng: Number(form.lng),
        rentAmount: Number(form.rentAmount),
        bedrooms: form.bedrooms ? Number(form.bedrooms) : null,
        bathrooms: form.bathrooms ? Number(form.bathrooms) : null,
      })
      navigate('/my-listings')
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="container page">
      <h1>Post a property</h1>
      <p className="text-muted">
        Submitted listings stay pending until our field team visits and verifies them.
      </p>
      <div className="spacer-md" />

      <form className="form form-wide" onSubmit={handleSubmit}>
        {error && <div className="error-box">{error}</div>}

        <div className="field">
          <label htmlFor="type">Listing type</label>
          <select id="type" value={form.type} onChange={(e) => update('type', e.target.value)}>
            <option value="RENT">Rental</option>
            <option value="PG">PG / Co-living</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="title">Title</label>
          <input
            id="title"
            placeholder="e.g. 2BHK near Infosys Mysuru campus"
            value={form.title}
            onChange={(e) => update('title', e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="addressLine">Address</label>
          <input
            id="addressLine"
            value={form.addressLine}
            onChange={(e) => update('addressLine', e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="locality">Locality</label>
          <input
            id="locality"
            placeholder="e.g. Vijayanagar"
            value={form.locality}
            onChange={(e) => update('locality', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="lat">Latitude</label>
            <input
              id="lat"
              type="number"
              step="any"
              placeholder="12.3244"
              value={form.lat}
              onChange={(e) => update('lat', e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="lng">Longitude</label>
            <input
              id="lng"
              type="number"
              step="any"
              placeholder="76.6183"
              value={form.lng}
              onChange={(e) => update('lng', e.target.value)}
              required
            />
          </div>
        </div>

        <div className="field">
          <label htmlFor="rentAmount">Monthly rent (₹)</label>
          <input
            id="rentAmount"
            type="number"
            min="0"
            value={form.rentAmount}
            onChange={(e) => update('rentAmount', e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="bedrooms">Bedrooms</label>
            <input
              id="bedrooms"
              type="number"
              min="0"
              value={form.bedrooms}
              onChange={(e) => update('bedrooms', e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="bathrooms">Bathrooms</label>
            <input
              id="bathrooms"
              type="number"
              min="0"
              value={form.bathrooms}
              onChange={(e) => update('bathrooms', e.target.value)}
            />
          </div>
        </div>

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Submitting…' : 'Submit for verification'}
        </button>
      </form>
    </div>
  )
}
