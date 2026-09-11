# Karnataka Real Estate Platform

Phase 1 build: owner-direct rentals and PG/co-living for one corridor in
Mysuru (Vijayanagar, Hebbal, Hootagalli, Bogadi, Dattagalli,
Ramakrishnanagar). See the strategy document for the full three-phase plan.

## Stack

- **Backend** — `backend/`: Java 21, Spring Boot 4.1, Spring Web, Spring Data
  JPA, Spring Security with JWT (email + password), MySQL 8.
- **Frontend** — `frontend/`: React 19, plain JavaScript (no TypeScript),
  plain CSS (no Tailwind), React Router.

## Running locally

### 1. Database (one-time setup)

You already have MySQL Server 8.0 + Workbench installed. Open Workbench,
connect to your local instance (root), open a new SQL tab, and run
[`backend/sql/setup.sql`](backend/sql/setup.sql) — it creates the
`real_estate` database and a dedicated `realestate_app` user, so the backend
never needs your root password. Safe to re-run if you're not sure whether
you already did it.

Don't have MySQL installed? `docker compose up -d` starts a MySQL 8
container instead (see `docker-compose.yml`) — then run the same SQL
against it.

### 2. Backend

```
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. Tables are created automatically on first
run (`ddl-auto: update`), and a few demo accounts + listings are seeded so
the app isn't empty (see "Demo accounts" below).

Health check: `GET http://localhost:8080/actuator/health`

### 3. Frontend

```
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`. Requests to `/api/*` are proxied to the
backend on `:8080` (see `vite.config.js`).

## Demo accounts

Seeded automatically the first time the backend starts against an empty
database:

| Email | Password | Role |
|---|---|---|
| admin@mysuruhomes.local | admin1234 | ADMIN — sees the moderation queue |
| owner@mysuruhomes.local | owner1234 | OWNER — posts properties, has 3 demo listings already |
| tenant@mysuruhomes.local | tenant1234 | TENANT — browses, enquires, books visits |

## Complete flow

**Tenant:** browse `/` (no login needed) → open a listing → log in/sign up
if prompted → "I'm interested" (enquiry) or book a visit with a date/time.

**Owner:** sign up as OWNER (or use the demo account) → "Post a property" →
fill the form → submitted as `DRAFT` (pending) → shows on "My listings"
with its status → once an admin verifies it, it flips to `LIVE` and
appears in public search.

**Admin:** log in as ADMIN → "Admin review" → sees every `DRAFT` listing →
"Mark verified" → listing becomes `LIVE`, `verifiedAt`/`verifiedBy` are
stamped (mirrors the strategy document's core differentiator: every
listing is confirmed before it's public).

See the "Page → API → table" mapping below for exactly what each screen
calls and what it touches in the database.

## Page → API → database mapping

| Frontend page | Calls | Backend controller | Table(s) touched |
|---|---|---|---|
| Home (`/`) | `GET /api/listings` | `ListingController` | `listings` |
| Listing detail (`/listings/:id`) | `GET /api/listings/{id}`, `POST /api/enquiries`, `POST /api/visits` | `ListingController`, `EnquiryController`, `VisitBookingController` | `listings`, `enquiries`, `visit_bookings` |
| Login (`/login`) | `POST /api/auth/login` | `AuthController` | `users` |
| Register (`/register`) | `POST /api/auth/register` | `AuthController` | `users` |
| Post a property (`/post-property`) | `POST /api/listings` | `ListingController` | `listings` |
| My listings (`/my-listings`) | `GET /api/my-listings` | `ListingController` | `listings` |
| Admin review (`/admin`) | `GET /api/admin/listings/pending`, `POST /api/admin/listings/{id}/verify` | `AdminController` | `listings`, `users` |

## Auth model

Email + password (not phone-OTP — that needs a paid SMS provider like
MSG91/Twilio, which is a Phase 2 swap-in, not a Phase 1 blocker). On
login/register the backend returns a JWT; the frontend stores it in
`localStorage` and sends it as `Authorization: Bearer <token>` on every
request after that (see `frontend/src/api/client.js` and
`frontend/src/context/AuthContext.jsx`). Route access:

- Public: browsing listings, login, register.
- Logged in (any role): enquiries, visit booking.
- `OWNER` only: posting a listing, "My listings".
- `ADMIN` only: the moderation queue.

## Project layout

```
real-estate project/
├── backend/
│   ├── sql/setup.sql              One-time MySQL database + user setup
│   └── src/main/java/com/realestate/api/
│       ├── auth/                  Register/login, JWT issuing
│       ├── security/              JWT filter + verification
│       ├── user/                  User entity, roles
│       ├── listing/               Listing entity, search, owner posting
│       ├── enquiry/                "I'm interested" taps
│       ├── visit/                 Visit-booking requests
│       ├── admin/                 Moderation queue
│       └── config/                Security rules, CORS, demo data, error handling
├── frontend/
│   └── src/
│       ├── api/client.js          All fetch() calls to the backend
│       ├── context/AuthContext.jsx  Who's logged in, JWT storage
│       ├── components/            Navbar, listing card, route guard
│       └── pages/                 One file per screen (see table above)
└── docker-compose.yml             Optional MySQL container
```

## What's built so far

| Area | Status |
|---|---|
| Domain model | `User`, `Listing`, `ListingPhoto`, `Enquiry`, `VisitBooking`, `LocalityPage` |
| Auth | Email+password register/login, JWT, role-gated routes |
| Listing search | Locality filter, geo-radius (Haversine), listing detail |
| Owner flow | Post a property, view own listings with status |
| Admin flow | Pending-listing queue, mark-verified action |
| Tenant actions | Enquiries, visit booking |
| Frontend | Full multi-page app (Home, detail, auth, owner, admin) in plain JS/CSS |

## Explicitly not built yet (by design — see Phase 1 scope)

Phone-OTP login (needs a paid SMS provider), photo upload (listings take an
image URL for now, not a file upload), WhatsApp notifications, payments,
mobile app, Flyway/Liquibase migrations (schema is auto-created via
`ddl-auto: update`, fine for local dev only). These are the next slices —
see the strategy document's "What gets built" table for Phase 1.
