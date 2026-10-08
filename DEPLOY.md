# Deploying Mysuru Homes

A beginner-friendly, click-by-click guide to putting this app on the public
internet for free, using:

- **Aiven** — managed MySQL database (free plan)
- **Render** — backend API, built from the `backend/Dockerfile` (free plan)
- **Netlify** — frontend static site (free plan)
- **Cloudinary** — optional, keeps uploaded listing photos from disappearing
  (free plan)

You don't need to write or change any code for this — everything the app
needs is already wired up to read from environment variables. You just need
to create accounts and fill in a few settings.

**Order matters** — set these up in the order below, because each later step
needs a value produced by an earlier one (e.g. Render needs Aiven's database
host before it can start; Netlify needs Render's URL).

Budget about 30–45 minutes for the first pass.

---

## 0. Push this repo to GitHub

Render and Netlify both deploy by connecting to a GitHub repository. This
project's repo is already at `https://github.com/hemanthkumar0114/mysuru-homes` —
make sure your latest commits (including the deployment changes) are pushed
there before continuing:

```
git push origin master
```

(If you're not there yet, come back to this step after Claude tells you
everything is committed.)

---

## 1. Aiven — create the MySQL database

1. Go to **[aiven.io](https://aiven.io)** and click **Get started free** (or
   **Sign up**). Sign up with email or GitHub/Google — no credit card is
   required for the free plan.
2. Verify your email if asked, then you'll land in the **Aiven Console**.
3. Click **Create service**.
4. Pick **MySQL** as the service type.
5. Under **Service plan**, choose the **Free** plan.
6. Pick any **cloud region** close to you (this only affects latency).
7. Give it a name, e.g. `mysuru-homes-db`, and click **Create service**.
8. Wait for the status indicator to turn solid green (a flashing/blinking
   dot means it's still being provisioned — usually 1-2 minutes).
9. Open the service and find the **Connection information** / **Overview**
   panel. Write down these five values — you'll need them for Render in
   step 3:
   - **Host**
   - **Port**
   - **User** (usually `avnadmin`)
   - **Password** (click "show"/the eye icon to reveal it)
   - The default database is called `defaultdb` — that's fine to use, or
     create a new database named `real_estate` from the **Databases** tab if
     you'd rather match this project's usual naming.

**Free plan limits to know about:** 1 GB RAM, 1 GB storage, capped
connections — plenty for a portfolio project, but Aiven reserves the right
to pause a free service that sees no activity for a long stretch. If your
app stops connecting after being idle for weeks, check the Aiven console
for a "paused" state and resume it there.

You do **not** need to run `backend/sql/setup.sql` against Aiven — that
script exists only to create a restricted local MySQL user for development.
On Aiven, use the `avnadmin` user Aiven already gave you; Flyway creates
all the tables from `backend/src/main/resources/db/migration` the first
time the app connects to an empty database.

---

## 2. Cloudinary — keep uploaded photos from disappearing (recommended)

Render's free plan wipes the backend's local disk every time it restarts or
redeploys (see step 3's note on this). Any owner-uploaded listing photos
saved to local disk would vanish. Cloudinary's free tier is the simplest
fix — it's a separate, persistent place to store the images, and the app
already has the code for it; you just need to switch it on with two values.

If you'd rather skip this for now, that's fine — see **"Skipping
Cloudinary"** at the end of this section.

1. Go to **[cloudinary.com](https://cloudinary.com)** and click **Sign up
   free**. No credit card required.
2. After signing in, your **Console/Dashboard** home page shows your
   **Cloud name** near the top — write it down.
3. Click **Settings** (gear icon) in the left navigation, then open the
   **Upload** tab.
4. Scroll to the **Upload presets** section. Click **Add upload preset**.
5. Set:
   - **Preset name** — anything, e.g. `mysuru_homes_listings`
   - **Signing Mode** — **Unsigned**
6. Click **Save**.

   This is safe here even though Cloudinary's own docs warn that unsigned
   presets are less secure for browser-side uploads — this app never sends
   the preset name to the browser. Photos are uploaded by your Spring Boot
   backend on the server side, so the preset name is only ever known to
   your Render service.

You now have two values for Render: the **Cloud name** and the **upload
preset name**.

**Skipping Cloudinary:** if you leave `CLOUDINARY_CLOUD_NAME` and
`CLOUDINARY_UPLOAD_PRESET` unset in Render, the app falls back to saving
photos on local disk exactly like it does in development. The app and every
other feature works fine either way — you'll just lose any uploaded photos
(not the listings themselves, just their photos) whenever the free Render
service restarts, redeploys, or spins down from inactivity. That's a
reasonable tradeoff for a portfolio demo if you'd rather not add a fourth
account.

---

## 3. Render — deploy the backend

1. Go to **[render.com](https://render.com)** and sign up (GitHub sign-in is
   the easiest, since you'll be connecting a GitHub repo anyway).
2. Click **New** → **Web Service**.
3. Connect your GitHub account if prompted, then select the
   `mysuru-homes` repository.
4. Configure the service:
   - **Name** — e.g. `mysuru-homes-api`
   - **Root Directory** — `backend` (important — this repo has both a
     `backend/` and `frontend/` folder; this tells Render to build and run
     from inside `backend/`)
   - **Language / Runtime** — **Docker**
   - **Dockerfile Path** — `Dockerfile` (relative to the Root Directory you
     just set, so this resolves to `backend/Dockerfile`)
   - **Instance Type** — **Free**
5. Before clicking create, scroll to **Environment Variables** and add each
   of these (**Add Environment Variable** for each one). Real secret values
   only go here — never in code or chat:

   | Key | Value |
   |---|---|
   | `DB_HOST` | the Aiven **Host** from step 1 |
   | `DB_PORT` | the Aiven **Port** from step 1 |
   | `DB_NAME` | `defaultdb` (or the database name you created in step 1) |
   | `DB_USER` | the Aiven **User** from step 1 (usually `avnadmin`) |
   | `DB_PASSWORD` | the Aiven **Password** from step 1 |
   | `JWT_SECRET` | a long random string (32+ characters) — generate one any way you like, e.g. a password manager's "generate password" feature |
   | `ADMIN_EMAIL` | the email you want to log in to the admin/moderation account with |
   | `ADMIN_PASSWORD` | a strong password for that admin account — this is the **only** way an admin account gets created, there's no default |
   | `ALLOWED_ORIGINS` | leave a placeholder for now, e.g. `https://placeholder.netlify.app` — you'll come back and fix this in step 5 once Netlify gives you a real URL |
   | `CLOUDINARY_CLOUD_NAME` | from step 2 (skip this row if you're skipping Cloudinary) |
   | `CLOUDINARY_UPLOAD_PRESET` | from step 2 (skip this row if you're skipping Cloudinary) |

   Optional, only if you also want demo owner/tenant accounts with a couple
   of sample listings (see [README.md](README.md#demo-accounts)):

   | Key | Value |
   |---|---|
   | `DEMO_OWNER_EMAIL` | any email |
   | `DEMO_OWNER_PASSWORD` | a password for it |
   | `DEMO_TENANT_EMAIL` | any email |
   | `DEMO_TENANT_PASSWORD` | a password for it |

6. Click **Create Web Service**. Render will clone the repo, build the
   Docker image (this takes a few minutes the first time), and start it.
7. Watch the **Logs** tab. A successful start looks like a Spring Boot
   banner followed by `Started ApiApplication`. If it fails, the log will
   usually say exactly which environment variable is missing or which
   database connection failed.
8. Once it's live, Render shows your service's public URL at the top of the
   page, e.g. `https://mysuru-homes-api.onrender.com`. Write this down —
   you need it for Netlify next.
9. Sanity check: open `https://<your-render-url>/actuator/health` in a
   browser. You should see `{"status":"UP"}`.

**About Render's free plan:** it spins the service down after 15 minutes
with no traffic, and spins back up (taking about a minute) on the next
request — so the very first visit after a quiet period will feel slow. This
is normal and free. Also, as mentioned above, its disk is wiped on every
restart/redeploy/spin-down — that's exactly the limitation Cloudinary (step
2) works around for photos; nothing else in the app is affected, since
everything besides photos lives in the Aiven database.

---

## 4. Netlify — deploy the frontend

1. Go to **[netlify.com](https://netlify.com)** and sign up (GitHub sign-in
   again is the easiest).
2. Click **Add new site** → **Import an existing project**.
3. Choose **GitHub**, authorize it, and select the `mysuru-homes` repo.
4. Netlify will try to auto-detect settings; make sure they read:
   - **Base directory** — `frontend`
   - **Build command** — `npm run build`
   - **Publish directory** — `frontend/dist`

   (`frontend/netlify.toml` already fills these in for you if Netlify reads
   it correctly, but double-check the fields above match before deploying.)
5. Before the first deploy, click **Add environment variables** (or find
   **Site settings → Environment variables** afterwards) and add:

   | Key | Value |
   |---|---|
   | `VITE_API_URL` | your Render URL from step 3, no trailing slash — e.g. `https://mysuru-homes-api.onrender.com` |

   Make sure its scope includes **Builds** (Vite reads this at build time,
   not while the site is running).
6. Click **Deploy**. Wait for the build to finish — Netlify shows a live
   log.
7. Once deployed, Netlify gives you a URL like
   `https://random-name-123abc.netlify.app`. You can rename this under
   **Site settings → Site details → Change site name** if you'd like a
   nicer URL.

---

## 5. Connect the two: fix CORS on Render

Now that you have your real Netlify URL:

1. Go back to your Render service → **Environment**.
2. Edit `ALLOWED_ORIGINS` and set it to your actual Netlify URL, e.g.
   `https://mysuru-homes.netlify.app` (no trailing slash). If you also want
   to keep testing against `localhost:5173`, separate multiple origins with
   commas: `https://mysuru-homes.netlify.app,http://localhost:5173`.
3. Save — Render will automatically redeploy with the new value.

---

## 6. Try it out

1. Open your Netlify URL.
2. You should see the home page with locality search (it will look empty
   of listings unless you set up demo accounts/data, or post one yourself).
3. Register a TENANT or OWNER account through the UI, or log in as
   `ADMIN_EMAIL` / `ADMIN_PASSWORD` from step 3.
4. As the OWNER, post a listing and upload a photo; as the ADMIN, verify it
   from the moderation queue; confirm it then shows up on the public
   listing search.
5. Refresh the page on a deep link like `/listings/<id>` directly (not by
   clicking through the app) to confirm Netlify's SPA redirect is working —
   you should see the listing, not a 404.

---

## Environment variable reference

**Backend (Render)**

| Variable | Required? | Purpose |
|---|---|---|
| `DB_HOST` | Yes | Aiven MySQL host |
| `DB_PORT` | Yes | Aiven MySQL port |
| `DB_NAME` | Yes | Database name |
| `DB_USER` | Yes | Database user |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Signing key for auth tokens (32+ random characters) |
| `ALLOWED_ORIGINS` | Yes | Comma-separated browser origins allowed to call the API (your Netlify URL) |
| `ADMIN_EMAIL` | No, but needed for any admin login | Creates the one admin account on first startup |
| `ADMIN_PASSWORD` | No, but needed for any admin login | Password for that admin account |
| `DEMO_OWNER_EMAIL` / `DEMO_OWNER_PASSWORD` | No | Optional demo OWNER account + sample listings |
| `DEMO_TENANT_EMAIL` / `DEMO_TENANT_PASSWORD` | No | Optional demo TENANT account |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_UPLOAD_PRESET` | No | Route uploaded photos to Cloudinary instead of local disk |
| `PORT` | No (Render sets it) | HTTP port the app listens on |
| `JWT_EXPIRATION_MS` | No (default 24h) | Token lifetime |
| `UPLOAD_DIR` | No | Local disk folder for photos when Cloudinary isn't configured |
| `SPRING_PROFILES_ACTIVE` | No (the Docker image sets `prod`) | The `prod` profile forces an encrypted database connection (`sslMode=REQUIRED`) and fails to start rather than falling back to plaintext |

**Frontend (Netlify)**

| Variable | Required? | Purpose |
|---|---|---|
| `VITE_API_URL` | Yes | Full URL of the deployed backend, no trailing slash |

No real values are listed here on purpose — fill them in directly on Render
and Netlify's dashboards, never in this file or in chat.

---

## Known limitations of this setup

- **Free tier cold starts** — Render's free plan sleeps after 15 minutes of
  inactivity; the next visitor waits ~1 minute for it to wake up.
- **Photos without Cloudinary** — if you skip step 2, uploaded listing
  photos are lost whenever Render restarts, redeploys, or spins down.
  Listings themselves (title, rent, location, etc.) are unaffected, since
  they live in the Aiven database, not on disk.
- **Aiven free plan** — capped resources (1 GB RAM/disk, ~76 connections),
  no SLA, and Aiven may pause a service that's been completely idle for a
  long time. Fine for a portfolio demo; not meant for real traffic.
- **Schema changes go through Flyway** — add a new
  `V2__what_changed.sql` file in `backend/src/main/resources/db/migration`;
  never edit a migration that has already run. An existing database with no
  Flyway history is baselined at V1 on first start.
