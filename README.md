# Karnataka Real Estate Platform

Phase 1 scaffold: owner-direct rentals and PG/co-living for one corridor in
Mysuru (Vijayanagar, Hebbal, Hootagalli, Bogadi, Dattagalli,
Ramakrishnanagar). See the strategy document for the full three-phase plan.

## Stack

- **Backend** — `backend/`: Java 21, Spring Boot 4.1, Spring Web, Spring Data
  JPA, Spring Security (permissive placeholder until OTP auth is built),
  PostgreSQL (PostGIS-ready).
- **Frontend** — `frontend/`: React 19, TypeScript, Vite, Tailwind CSS v4.

## Running locally

### 1. Database

```
docker compose up -d
```

Starts Postgres (PostGIS image) on `localhost:5432`, database `real_estate`,
user/password `postgres`/`postgres`. Override via `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER`, `DB_PASSWORD` env vars if you point at something else.

### 2. Backend

```
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. Tables are created automatically
(`ddl-auto: update`) — fine for local dev, replace with Flyway/Liquibase
migrations before this touches a shared database.

Health check: `GET http://localhost:8080/actuator/health`

### 3. Frontend

```
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`. Requests to `/api/*` are proxied to the
backend on `:8080` (see `vite.config.ts`), so no CORS setup is needed in dev.

## What's built so far

| Area | Status |
|---|---|
| Domain model | `User`, `Listing`, `ListingPhoto`, `Enquiry`, `VisitBooking`, `LocalityPage` entities |
| Listing search | `GET /api/listings` — by locality, or by lat/lng/radiusKm (Haversine, no PostGIS yet) |
| Listing detail | `GET /api/listings/{id}` |
| Enquiries | `POST /api/enquiries` |
| Frontend | Landing page pulling live listings from the API |

## Explicitly not built yet (by design — see Phase 1 scope)

Auth (phone OTP), owner posting flow, visit-booking endpoints, admin
moderation queue, WhatsApp notifications, payments, mobile app. These are
the next slices, in roughly that order — see the strategy document's
"What gets built" table for Phase 1.

## Project layout

```
real-estate-project/
├── backend/    Spring Boot API (Maven)
├── frontend/   React + Vite + TypeScript
└── docker-compose.yml   Local Postgres/PostGIS
```
