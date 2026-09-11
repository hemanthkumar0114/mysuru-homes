import { useEffect, useState } from 'react'
import { fetchListings, type ListingSummary } from './api/client'

function App() {
  const [listings, setListings] = useState<ListingSummary[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>(
    'loading',
  )

  useEffect(() => {
    fetchListings()
      .then((data) => {
        setListings(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [])

  return (
    <div className="min-h-screen bg-white text-slate-900">
      <header className="border-b border-slate-200">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4">
          <span className="text-lg font-semibold">Mysuru Homes</span>
          <nav className="flex gap-4 text-sm text-slate-600">
            <a href="/">Rent</a>
            <a href="/">PG / Co-living</a>
            <a href="/">Post a property</a>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-16">
        <div className="text-center">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Verified rentals in Mysuru, owner-direct.
          </h1>
          <p className="mx-auto mt-3 max-w-xl text-slate-600">
            Every listing is physically visited and photographed by our
            team. No broker spam.
          </p>
        </div>

        <section className="mt-12">
          {status === 'loading' && (
            <p className="text-center text-slate-500">Loading listings…</p>
          )}
          {status === 'error' && (
            <p className="text-center text-red-600">
              Could not reach the API. Is the backend running on :8080?
            </p>
          )}
          {status === 'ready' && listings.length === 0 && (
            <p className="text-center text-slate-500">
              No live listings yet — this is expected on a fresh database.
            </p>
          )}
          {status === 'ready' && listings.length > 0 && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {listings.map((listing) => (
                <article
                  key={listing.id}
                  className="rounded-lg border border-slate-200 p-4 text-left"
                >
                  <h2 className="font-medium">{listing.title}</h2>
                  <p className="text-sm text-slate-500">
                    {listing.locality} · {listing.type}
                  </p>
                  <p className="mt-2 font-semibold">
                    ₹{listing.rentAmount.toLocaleString('en-IN')}/mo
                  </p>
                  {listing.verified && (
                    <span className="mt-2 inline-block rounded bg-emerald-100 px-2 py-0.5 text-xs text-emerald-700">
                      Verified
                    </span>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default App
