# Mysuru Homes

A full-stack rental listings platform for one corridor of Mysuru, Karnataka.
Owners post properties directly (no brokers), a field-verification step
keeps listings honest, and tenants browse, enquire and book visits — all
backed by a real MySQL database, JWT auth, and role-based access control.

Built as a portfolio project to demonstrate a complete, working full-stack
application: a Spring Boot REST API, a React frontend with no build-tool
scaffolding left un-customized, real authorization rules (not just
authentication), file uploads, and an automated test suite that runs with
one command.

## Features

- **Public browsing** — search live listings by locality, type (rental/PG),
  rent range and bedroom count, or by radius around a point (Haversine
  distance on plain lat/lng columns). Six locality landing pages
  (Vijayanagar, Hebbal, Hootagalli, Bogadi, Dattagalli, Ramakrishnanagar)
  show live rent ranges and listings for that area.
- **Owner-direct listings** — an owner posts a property with up to 5 photos;
  it starts as `DRAFT` and is invisible to everyone but its owner and admins
  until a field team member verifies it.
- **Field verification** — an admin moderation queue lists every pending
  `DRAFT`; verifying one stamps `verifiedAt`/`verifiedBy` and makes it
  `LIVE` and publicly searchable. This is the platform's core trust
  mechanic — no unverified listing is ever shown to the public.
- **Enquiries** — a logged-in tenant can tell an owner "I'm interested" on
  any `LIVE` listing, once per listing.
- **Visit booking** — a tenant requests a visit slot (IST date/time) on any
  `LIVE` listing, one open request per listing at a time, and can cancel it
  from "My visits". Admins see every request, filterable by status, and
  confirm or cancel them.
- **Owner dashboard** — "My listings" shows enquiry and visit-request counts
  per property, with a detail view listing who enquired and who asked to
  visit (tenants shown by first name only).
- **Photo uploads** — up to 5 JPEG/PNG/WebP photos per listing, validated
  server-side for type and size, shown on listing cards and a clickable
  gallery on the detail page.
- **Auth & authorization** — email/password with JWTs; every sensitive
  action is checked against the caller's role *and* ownership (not just
  "are you logged in"), enforced in the controllers and covered by tests.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Web, Spring Data JPA, Spring Security (JWT via `jjwt`) |
| Database | MySQL 8 (production/dev) — automated tests use an in-memory H2 database instead, see [Running tests](#running-tests) |
| Frontend | React 19, React Router, plain JavaScript (no TypeScript), plain CSS (no Tailwind/CSS framework), Vite |
| Testing | JUnit 5 + Spring Boot Test (backend, HTTP-level integration tests), Vitest (frontend, unit tests) |

## Architecture

```
┌─────────────────────┐        HTTP (JSON, JWT bearer token)       ┌──────────────────────────────┐
│   React SPA (Vite)   │ ───────────────────────────────────────▶  │   Spring Boot REST API        │
│   :5173 in dev        │ ◀───────────────────────────────────────  │   :8080                       │
│                       │        /api/* proxied by Vite in dev      │                                │
│  pages/  components/  │                                            │  auth/      JWT issue+verify   │
│  context/AuthContext  │                                            │  listing/   search, CRUD,      │
│   (JWT in             │                                            │             photos, activity   │
│    localStorage)      │                                            │  enquiry/   "I'm interested"   │
└──────────┬────────────┘                                            │  visit/     visit bookings     │
           │                                                          │  admin/     moderation queue,  │
           │ GET /uploads/**                                          │             visit management   │
           │ (photo files)                                            │  locality/  locality pages     │
           ▼                                                          │  security/  JWT filter,        │
┌─────────────────────┐                                               │             AuthenticatedUser  │
│  local uploads/       │◀──────────────────────────────────────────  │  config/    SecurityConfig,    │
│  folder (dev storage) │        files saved by PhotoStorageService   │             error handling      │
└─────────────────────┘                                               └───────────────┬──────────────┘
                                                                                        │ Spring Data JPA
                                                                                        ▼
                                                                       ┌──────────────────────────────┐
                                                                       │   MySQL 8 (dev/prod)          │
                                                                       │   users, listings,             │
                                                                       │   listing_photos, enquiries,   │
                                                                       │   visit_bookings,              │
                                                                       │   locality_pages               │
                                                                       └──────────────────────────────┘
```

Every request after login carries `Authorization: Bearer <jwt>`; there are
no server-side sessions (`SessionCreationPolicy.STATELESS`). `JwtAuthFilter`
decodes the token into an `AuthenticatedUser` principal that controllers
read directly — no extra database round-trip just to know who's calling.
Authorization is layered: route-level role rules in `SecurityConfig`
(e.g. only `OWNER` can `POST /api/listings`), plus per-resource ownership
checks inside the controllers themselves (e.g. an owner can only see
activity for *their own* listing — a role alone can't express that).

## Getting started

### Prerequisites

- Java 21 (JDK)
- Node.js 20+ and npm
- MySQL 8 Server (a `docker-compose.yml` is included if you'd rather run
  MySQL in a container)

### 1. Database

Run [`backend/sql/setup.sql`](backend/sql/setup.sql) once against your
MySQL server (MySQL Workbench, or `mysql -u root -p < backend/sql/setup.sql`
from the CLI as root). It creates the `real_estate` database and a
dedicated `realestate_app` user — edit the placeholder password in that
file first, and use the same value for `DB_PASSWORD` below. The backend
never needs your root credentials after this.

No local MySQL? `docker compose up -d` starts one (see
`docker-compose.yml`), then run the same SQL against it.

### 2. Environment variables

The backend refuses to start unless these are set — there are no baked-in
defaults for secrets. Copy [`backend/.env.example`](backend/.env.example)
as a reference and set these as real OS environment variables (Spring Boot
reads them directly; there's no `.env` file loader wired up).

| Variable | Required | Purpose |
|---|---|---|
| `DB_PASSWORD` | Yes | Password for the `realestate_app` MySQL user (see step 1) |
| `JWT_SECRET` | Yes | Signing key for auth tokens — any long random string (32+ chars). Generate one with, e.g., `-join ((48..57)+(65..90)+(97..122)\|Get-Random -Count 48\|%{[char]$_})` in PowerShell |
| `DB_HOST` | No (default `localhost`) | MySQL host |
| `DB_PORT` | No (default `3306`) | MySQL port |
| `DB_NAME` | No (default `real_estate`) | Database name |
| `DB_USER` | No (default `realestate_app`) | Database user |
| `PORT` | No (default `8080`) | Backend HTTP port |
| `JWT_EXPIRATION_MS` | No (default `86400000` = 24h) | Token lifetime |
| `UPLOAD_DIR` | No (default `uploads`) | Where listing photos are stored on disk |
| `ALLOWED_ORIGINS` | No (default `http://localhost:5173`) | Comma-separated browser origins allowed to call the API (CORS) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | No | Creates the one admin account on first run — see [Demo accounts](#demo-accounts) |
| `DEMO_OWNER_EMAIL` / `DEMO_OWNER_PASSWORD` | No | Optional demo OWNER account + sample listings |
| `DEMO_TENANT_EMAIL` / `DEMO_TENANT_PASSWORD` | No | Optional demo TENANT account |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_UPLOAD_PRESET` | No | Route uploaded photos to Cloudinary instead of local disk (see [DEPLOY.md](DEPLOY.md)) |

PowerShell (per terminal session):
```powershell
$env:DB_PASSWORD = "your-password-from-setup.sql"
$env:JWT_SECRET = "a-long-random-string-at-least-32-characters"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "pick-your-own-password"
```

Deploying this publicly? See [DEPLOY.md](DEPLOY.md) for the full Aiven/Render/Netlify walkthrough.

### 3. Backend

```
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. Tables are created on first run by Flyway
migrations (`backend/src/main/resources/db/migration`), and demo accounts + a few demo listings are
seeded so the app isn't empty (see [Demo accounts](#demo-accounts)).
Locality pages are seeded the same way.

Health check: `GET http://localhost:8080/actuator/health`

### 4. Frontend

```
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`. Requests to `/api/*` and `/uploads/*` are
proxied to the backend on `:8080` (see `frontend/vite.config.js`).

## Demo accounts

There are no hardcoded demo passwords — a public deployment must not ship a
guessable admin login. Instead, `DemoDataSeeder` creates accounts from
environment variables the first time the backend starts against an empty
database, and only creates the ones whose env vars you actually set:

| Env vars | Role | Notes |
|---|---|---|
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | ADMIN | Moderation queue, visit requests. The only way to get an admin account — it can't be created by registering through the UI. |
| `DEMO_OWNER_EMAIL` / `DEMO_OWNER_PASSWORD` | OWNER | Comes with a couple of sample listings (one already verified/LIVE, one still DRAFT) - only seeded if *both* the owner and admin pairs above are set. |
| `DEMO_TENANT_EMAIL` / `DEMO_TENANT_PASSWORD` | TENANT | Browse, enquire, book/cancel visits. |

For local dev, set at least `ADMIN_EMAIL`/`ADMIN_PASSWORD` alongside
`DB_PASSWORD`/`JWT_SECRET` in step 2 above - otherwise you'll have no way to
reach the admin moderation queue (TENANT/OWNER accounts can still be created
normally by registering through the UI).

## Running tests

Everything runs with **one command** and needs no database setup, no
environment variables, and never touches your real MySQL data — backend
tests run against an isolated in-memory H2 database created fresh per run
(see `backend/src/test/resources/application.yml`), and frontend tests are
pure unit tests.

```
./run-tests.sh      # macOS/Linux/Git-Bash
.\run-tests.ps1      # Windows PowerShell
```

Or run each suite on its own:

```
cd backend && ./mvnw test     # 51 tests: auth, search/filters, draft
                               # visibility, enquiry/visit rules, admin
                               # verification — real HTTP calls through
                               # the actual security filter chain
cd frontend && npm test        # 31 tests: formatRent, IST time helpers,
                               # visit-status/pluralization utilities
```

## Project structure

```
real-estate project/
├── run-tests.sh / run-tests.ps1   Run the whole test suite in one command
├── DEPLOY.md                       Aiven/Render/Netlify deployment walkthrough
├── backend/
│   ├── Dockerfile                    Multi-stage build for Render
│   ├── sql/setup.sql                One-time MySQL database + user setup
│   ├── .env.example                  Reference for required env vars
│   └── src/
│       ├── main/java/com/realestate/api/
│       │   ├── auth/        Register/login, JWT issuing
│       │   ├── security/    JWT filter, AuthenticatedUser principal
│       │   ├── user/        User entity, roles
│       │   ├── listing/     Search/filters, draft visibility, owner
│       │   │                 posting, photo uploads, owner activity
│       │   ├── enquiry/     "I'm interested" (LIVE-only, one per tenant)
│       │   ├── visit/       Visit booking, cancellation
│       │   ├── admin/       Moderation queue, visit confirm/cancel
│       │   ├── locality/    Public locality landing pages
│       │   └── config/      Security rules, CORS, uploads route,
│       │                     demo/locality seeding, error handling
│       └── test/java/com/realestate/api/   JUnit test suite (see above)
├── frontend/
│   ├── netlify.toml                   Build settings + SPA redirect for Netlify
│   └── src/
│       ├── api/client.js             Every fetch() call to the backend, VITE_API_URL
│       ├── context/AuthContext.jsx    Who's logged in, JWT storage
│       ├── components/                Navbar, listing card, route guard, ...
│       ├── pages/                     One file per screen
│       └── utils/                     formatRent, IST time helpers (tested)
└── docker-compose.yml                 Optional MySQL container
```

## Screenshots

_Add screenshots here before sharing this repo — e.g. drop image files into
`docs/screenshots/` and reference them below:_

- Home page with locality search and filters
- Listing detail page with photo gallery
- Post a property (owner) with photo upload
- Admin moderation queue and visit requests
- My visits (tenant) and My listings with enquiry/visit counts

```markdown
![Home page](docs/screenshots/home.png)
![Listing detail](docs/screenshots/listing-detail.png)
```

## Known limitations

- Phone-OTP login is out of scope (needs a paid SMS provider like
  MSG91/Twilio) — email/password only for now.
- Listing photos are stored on local disk by default (see
  `PhotoStorageService`) — fine for one server, but a host with ephemeral
  disk (e.g. Render's free tier) loses them on every restart. Set
  `CLOUDINARY_CLOUD_NAME`/`CLOUDINARY_UPLOAD_PRESET` to route photos to
  Cloudinary's free tier instead (see [DEPLOY.md](DEPLOY.md)); either way,
  this would need to move to a proper object store (S3/Cloud
  Storage/Cloudinary) before running on more than one instance.
- No WhatsApp notifications or payments (both explicitly out of scope for
  this phase).
