import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createListing, uploadListingPhotos } from '../api/client'
import { CheckIcon } from '../components/icons'

const MAX_PHOTOS = 5
const MAX_PHOTO_BYTES = 3 * 1024 * 1024 // must match PhotoStorageService on the backend
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp']

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

const TYPES = [
  { value: 'RENT', title: 'Rental', description: 'A flat or house for a tenant or family' },
  { value: 'PG', title: 'PG / Co-living', description: 'Rooms or beds for sharing' },
]

const NEXT_STEPS = [
  'You submit the details here. Your listing is saved as pending.',
  'Our field team visits the property and photographs it.',
  'Once verified, it goes live and tenants can find it.',
]

export default function PostProperty() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [photos, setPhotos] = useState([]) // File[], at most MAX_PHOTOS
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // One object URL per photo, for the thumbnails below. Recomputed only when the
  // chosen files change (not on every render, which would leak a URL each time).
  const previews = useMemo(() => photos.map((file) => URL.createObjectURL(file)), [photos])

  // An object URL keeps its file's data in memory until revoked - free the previous
  // batch whenever `previews` is replaced (including on unmount).
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function handlePhotoChange(e) {
    const chosen = Array.from(e.target.files)
    e.target.value = '' // lets the same file(s) be picked again after removing one

    if (photos.length + chosen.length > MAX_PHOTOS) {
      setError(`You can upload up to ${MAX_PHOTOS} photos.`)
      return
    }
    const badType = chosen.find((f) => !ALLOWED_PHOTO_TYPES.includes(f.type))
    if (badType) {
      setError(`"${badType.name}" isn't a JPEG, PNG or WebP image.`)
      return
    }
    const tooBig = chosen.find((f) => f.size > MAX_PHOTO_BYTES)
    if (tooBig) {
      setError(`"${tooBig.name}" is larger than 3MB.`)
      return
    }
    setError('')
    setPhotos((prev) => [...prev, ...chosen])
  }

  function removePhoto(index) {
    setPhotos((prev) => prev.filter((_, i) => i !== index))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const listing = await createListing({
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

      // The listing is saved either way - a photo upload failure shouldn't lose it.
      // It just goes to "My listings" with a note instead of a clean success message.
      let photoWarning = ''
      if (photos.length > 0) {
        try {
          await uploadListingPhotos(listing.id, photos)
        } catch (err) {
          photoWarning = ` Your photos didn't upload though: ${err.message}`
        }
      }

      // "state" is a note handed to the next page along with the navigation.
      navigate('/my-listings', { state: { posted: form.title, photoWarning } })
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
        Tell us about your property. Every listing is verified by our field team before it goes live.
      </p>
      <div className="spacer-md" />

      <div className="post-layout">
        <form className="card post-form" onSubmit={handleSubmit}>
          {error && (
            <div className="error-box" role="alert">
              {error}
            </div>
          )}

          <section className="form-section">
            <h2>Basics</h2>

            <fieldset className="choice-picker">
              <legend>Listing type</legend>
              <div className="choice-options">
                {TYPES.map((option) => (
                  <label
                    key={option.value}
                    className={form.type === option.value ? 'choice-option selected' : 'choice-option'}
                  >
                    <input
                      type="radio"
                      name="type"
                      value={option.value}
                      checked={form.type === option.value}
                      onChange={() => update('type', option.value)}
                    />
                    <span className="choice-title">
                      {option.title}
                      {form.type === option.value && <CheckIcon />}
                    </span>
                    <span className="choice-desc">{option.description}</span>
                  </label>
                ))}
              </div>
            </fieldset>

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
          </section>

          <section className="form-section">
            <h2>Location</h2>

            <div className="field">
              <label htmlFor="addressLine">Address</label>
              <input
                id="addressLine"
                autoComplete="street-address"
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
            <p className="field-hint text-muted">
              Tip: on Google Maps, right-click the exact spot and click the numbers at the top of
              the menu to copy them.
            </p>
          </section>

          <section className="form-section">
            <h2>Rent and details</h2>

            <div className="field">
              <label htmlFor="rentAmount">Monthly rent</label>
              <div className="input-prefix">
                <span aria-hidden="true">₹</span>
                <input
                  id="rentAmount"
                  type="number"
                  min="1"
                  placeholder="e.g. 12000"
                  value={form.rentAmount}
                  onChange={(e) => update('rentAmount', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="field">
                <label htmlFor="bedrooms">Bedrooms (optional)</label>
                <input
                  id="bedrooms"
                  type="number"
                  min="0"
                  value={form.bedrooms}
                  onChange={(e) => update('bedrooms', e.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="bathrooms">Bathrooms (optional)</label>
                <input
                  id="bathrooms"
                  type="number"
                  min="0"
                  value={form.bathrooms}
                  onChange={(e) => update('bathrooms', e.target.value)}
                />
              </div>
            </div>
          </section>

          <section className="form-section">
            <h2>Photos (optional)</h2>
            <p className="text-muted text-sm">
              Up to {MAX_PHOTOS} photos, JPEG/PNG/WebP, 3MB each. Our field team will still take
              their own photos when they verify the property.
            </p>

            {photos.length > 0 && (
              <ul className="photo-picker-list">
                {photos.map((file, index) => (
                  <li key={`${file.name}-${index}`} className="photo-picker-item">
                    <img src={previews[index]} alt="" />
                    <button
                      type="button"
                      className="photo-picker-remove"
                      aria-label={`Remove ${file.name}`}
                      onClick={() => removePhoto(index)}
                    >
                      &times;
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {photos.length < MAX_PHOTOS && (
              <div className="field">
                <label htmlFor="photos">Add photos</label>
                <input
                  id="photos"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  onChange={handlePhotoChange}
                />
              </div>
            )}
          </section>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit for verification'}
          </button>
        </form>

        <aside className="card post-side">
          <h2>What happens next</h2>
          <ol className="steps">
            {NEXT_STEPS.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          <p className="text-muted text-sm">
            You can follow the status of every property under &quot;My listings&quot;.
          </p>
        </aside>
      </div>
    </div>
  )
}
