#!/usr/bin/env node
// Bulk-creates sample listings on the deployed Mysuru Homes API through its
// public HTTP endpoints: owner login -> create listing -> optional photos ->
// admin login -> verify (so the listing goes LIVE).
//
// Requires Node 18+ (uses global fetch/FormData/Blob/AbortSignal.timeout).
// Reads credentials only from environment variables - see README-SEED.md
// (or the assistant's setup instructions) for how to set them.
//
// Usage:
//   node scripts/seed-live-listings.mjs

import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const API_BASE = process.env.MH_API_BASE || 'https://mysuru-homes-api.onrender.com';
const HEALTH_URL = `${API_BASE}/actuator/health`;
const WAKE_TIMEOUT_MS = 3 * 60 * 1000;
const WAKE_POLL_INTERVAL_MS = 5000;

const SCRIPT_DIR = path.dirname(fileURLToPath(import.meta.url));
const SEED_PHOTOS_DIR = path.join(SCRIPT_DIR, 'seed-photos');
const MAX_PHOTOS_PER_LISTING = 2;
const PHOTO_CONTENT_TYPES = {
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.webp': 'image/webp',
};

// ---- Sample listing data ---------------------------------------------
// `locality` must start with one of the six seeded locality names so the
// listing shows up on that locality's page (matched by prefix server-side).
// Coordinates are approximate/plausible points within each real Mysuru
// locality; addresses are fictional street/layout names.

const SAMPLE_LISTINGS = [
  {
    type: 'RENT',
    title: 'Sunlit 2BHK Apartment Near Vijayanagar Ring Road',
    locality: 'Vijayanagar 2nd Stage',
    addressLine: 'No. 14, 3rd Cross, Sharada Layout, Vijayanagar',
    lat: 12.3238, lng: 76.6214,
    rentAmount: 14000, bedrooms: 2, bathrooms: 2,
  },
  {
    type: 'PG',
    title: 'Comfort Stay PG for Working Women, Vijayanagar',
    locality: 'Vijayanagar 1st Stage',
    addressLine: 'No. 27, Kuvempunagar Extension Road, Vijayanagar',
    lat: 12.3251, lng: 76.6198,
    rentAmount: 7500, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Affordable 1BHK Studio in Vijayanagar 4th Stage',
    locality: 'Vijayanagar 4th Stage',
    addressLine: 'No. 8, Ganapathi Temple Road, Vijayanagar 4th Stage',
    lat: 12.3202, lng: 76.6266,
    rentAmount: 9000, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Spacious 3BHK Villa in Hebbal Industrial Layout',
    locality: 'Hebbal Industrial Area',
    addressLine: 'No. 45, 2nd Main, Hebbal Industrial Layout',
    lat: 12.3402, lng: 76.6158,
    rentAmount: 22000, bedrooms: 3, bathrooms: 3,
  },
  {
    type: 'RENT',
    title: 'Bright 2BHK Apartment in Hebbal 2nd Stage',
    locality: 'Hebbal 2nd Stage',
    addressLine: 'No. 19, Vivekananda Road, Hebbal 2nd Stage',
    lat: 12.3378, lng: 76.6203,
    rentAmount: 15000, bedrooms: 2, bathrooms: 2,
  },
  {
    type: 'PG',
    title: 'Hebbal Tech Park PG for Men',
    locality: 'Hebbal',
    addressLine: 'No. 3, Industrial Suburb Road, Hebbal',
    lat: 12.3415, lng: 76.6180,
    rentAmount: 8000, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Cozy 1BHK Near Hootagalli Industrial Area',
    locality: 'Hootagalli',
    addressLine: 'No. 22, KRS Road, Hootagalli',
    lat: 12.3459, lng: 76.6608,
    rentAmount: 9500, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Modern 2BHK Close to Hootagalli Infosys Campus',
    locality: 'Hootagalli Industrial Layout',
    addressLine: 'No. 61, Bannur Road Extension, Hootagalli',
    lat: 12.3481, lng: 76.6641,
    rentAmount: 16000, bedrooms: 2, bathrooms: 2,
  },
  {
    type: 'PG',
    title: 'Hootagalli PG Near Factory Zone',
    locality: 'Hootagalli',
    addressLine: 'No. 9, Belawadi Main Road, Hootagalli',
    lat: 12.3446, lng: 76.6599,
    rentAmount: 6800, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Quiet 2BHK Family Home in Bogadi',
    locality: 'Bogadi',
    addressLine: 'No. 33, Kabini Layout, Bogadi',
    lat: 12.2778, lng: 76.5781,
    rentAmount: 12000, bedrooms: 2, bathrooms: 1,
  },
  {
    type: 'PG',
    title: 'Bogadi Riverside PG for Students',
    locality: 'Bogadi 2nd Stage',
    addressLine: 'No. 5, River View Road, Bogadi 2nd Stage',
    lat: 12.2741, lng: 76.5739,
    rentAmount: 6500, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Well-Ventilated 3BHK in Dattagalli 3rd Stage',
    locality: 'Dattagalli 3rd Stage',
    addressLine: 'No. 71, Metagalli Ring Road, Dattagalli 3rd Stage',
    lat: 12.2989, lng: 76.6112,
    rentAmount: 19000, bedrooms: 3, bathrooms: 2,
  },
  {
    type: 'RENT',
    title: 'Budget 1BHK Near Dattagalli Bus Stand',
    locality: 'Dattagalli',
    addressLine: 'No. 16, Bus Stand Road, Dattagalli',
    lat: 12.2951, lng: 76.6079,
    rentAmount: 8500, bedrooms: 1, bathrooms: 1,
  },
  {
    type: 'RENT',
    title: 'Family-Friendly 2BHK in Ramakrishnanagar',
    locality: 'Ramakrishnanagar',
    addressLine: 'No. 28, Temple Street, Ramakrishnanagar',
    lat: 12.2733, lng: 76.6231,
    rentAmount: 13000, bedrooms: 2, bathrooms: 2,
  },
  {
    type: 'PG',
    title: 'Ramakrishnanagar PG for Working Professionals',
    locality: 'Ramakrishnanagar',
    addressLine: 'No. 4, Lake View Road, Ramakrishnanagar',
    lat: 12.2705, lng: 76.6198,
    rentAmount: 7000, bedrooms: 1, bathrooms: 1,
  },
];

// ---- Helpers ------------------------------------------------------------

function requireEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForBackend() {
  const deadline = Date.now() + WAKE_TIMEOUT_MS;
  let attempt = 0;
  while (Date.now() < deadline) {
    attempt += 1;
    try {
      const res = await fetch(HEALTH_URL, { signal: AbortSignal.timeout(15000) });
      if (res.ok) {
        const body = await res.json().catch(() => null);
        if (body && body.status === 'UP') {
          console.log(`Backend is awake (attempt ${attempt}).`);
          return;
        }
      }
      console.log(`Attempt ${attempt}: backend responded but not healthy yet, retrying...`);
    } catch {
      console.log(`Attempt ${attempt}: backend not reachable yet (likely waking from sleep), retrying...`);
    }
    await sleep(WAKE_POLL_INTERVAL_MS);
  }
  throw new Error('Backend did not become healthy within 3 minutes - is the Render service down?');
}

async function login(email, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    throw new Error(`Login failed with status ${res.status}: ${await res.text()}`);
  }
  const data = await res.json();
  return data.token;
}

async function getOwnerListingTitles(ownerToken) {
  const res = await fetch(`${API_BASE}/api/my-listings`, {
    headers: { Authorization: `Bearer ${ownerToken}` },
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch owner listings: ${res.status} ${await res.text()}`);
  }
  const listings = await res.json();
  return new Set(listings.map((listing) => listing.title));
}

async function createListing(ownerToken, listing) {
  const res = await fetch(`${API_BASE}/api/listings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${ownerToken}`,
    },
    body: JSON.stringify({
      type: listing.type,
      title: listing.title,
      addressLine: listing.addressLine,
      locality: listing.locality,
      lat: listing.lat,
      lng: listing.lng,
      rentAmount: listing.rentAmount,
      bedrooms: listing.bedrooms,
      bathrooms: listing.bathrooms,
    }),
  });
  if (!res.ok) {
    throw new Error(`Create listing failed: ${res.status} ${await res.text()}`);
  }
  return res.json();
}

async function listSeedPhotoFiles() {
  try {
    const entries = await readdir(SEED_PHOTOS_DIR, { withFileTypes: true });
    const files = [];
    for (const entry of entries) {
      if (!entry.isFile()) continue;
      const ext = path.extname(entry.name).toLowerCase();
      if (PHOTO_CONTENT_TYPES[ext]) {
        files.push(path.join(SEED_PHOTOS_DIR, entry.name));
      }
    }
    return files.sort();
  } catch {
    return [];
  }
}

async function uploadPhotos(ownerToken, listingId, filePaths) {
  if (filePaths.length === 0) return;
  const form = new FormData();
  for (const filePath of filePaths) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = PHOTO_CONTENT_TYPES[ext];
    const buffer = await readFile(filePath);
    const blob = new Blob([buffer], { type: contentType });
    form.append('files', blob, path.basename(filePath));
  }
  const res = await fetch(`${API_BASE}/api/listings/${listingId}/photos`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${ownerToken}` },
    body: form,
  });
  if (!res.ok) {
    throw new Error(`Photo upload failed: ${res.status} ${await res.text()}`);
  }
}

async function verifyListing(adminToken, listingId) {
  const res = await fetch(`${API_BASE}/api/admin/listings/${listingId}/verify`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  if (!res.ok) {
    throw new Error(`Verify failed: ${res.status} ${await res.text()}`);
  }
  return res.json();
}

// ---- Main -----------------------------------------------------------------

async function main() {
  const ownerEmail = requireEnv('MH_OWNER_EMAIL');
  const ownerPassword = requireEnv('MH_OWNER_PASSWORD');
  const adminEmail = requireEnv('MH_ADMIN_EMAIL');
  const adminPassword = requireEnv('MH_ADMIN_PASSWORD');

  console.log(`Target API: ${API_BASE}`);
  console.log('Waking backend (Render free tier can take up to a minute to spin up)...');
  await waitForBackend();

  console.log('Logging in as owner...');
  const ownerToken = await login(ownerEmail, ownerPassword);
  console.log('Logging in as admin...');
  const adminToken = await login(adminEmail, adminPassword);

  console.log('Fetching existing owner listings for de-duplication...');
  const existingTitles = await getOwnerListingTitles(ownerToken);

  const photoFiles = await listSeedPhotoFiles();
  if (photoFiles.length > 0) {
    console.log(`Found ${photoFiles.length} seed photo(s) in scripts/seed-photos - will attach up to ${MAX_PHOTOS_PER_LISTING} per listing.`);
  } else {
    console.log('No usable photos in scripts/seed-photos (missing, empty, or wrong file type) - listings will be created without photos.');
  }

  let created = 0;
  let skipped = 0;
  let failed = 0;

  for (let i = 0; i < SAMPLE_LISTINGS.length; i += 1) {
    const listing = SAMPLE_LISTINGS[i];
    if (existingTitles.has(listing.title)) {
      console.log(`Skipping "${listing.title}" (already exists in owner's listings).`);
      skipped += 1;
      continue;
    }

    try {
      console.log(`Creating "${listing.title}"...`);
      const createdListing = await createListing(ownerToken, listing);

      if (photoFiles.length > 0) {
        const count = Math.min(MAX_PHOTOS_PER_LISTING, photoFiles.length);
        const offset = (i * MAX_PHOTOS_PER_LISTING) % photoFiles.length;
        const chosen = Array.from({ length: count }, (_, j) => photoFiles[(offset + j) % photoFiles.length]);
        try {
          await uploadPhotos(ownerToken, createdListing.id, chosen);
          console.log(`  Uploaded ${chosen.length} photo(s).`);
        } catch (photoErr) {
          console.warn(`  Photo upload failed, continuing without photos: ${photoErr.message}`);
        }
      }

      await verifyListing(adminToken, createdListing.id);
      console.log('  Verified - now LIVE.');
      created += 1;
    } catch (err) {
      console.error(`  Failed: ${err.message}`);
      failed += 1;
    }
  }

  console.log('');
  console.log(`Done. Created & verified: ${created}, skipped (duplicates): ${skipped}, failed: ${failed}.`);
}

main().catch((err) => {
  console.error(`Fatal error: ${err.message}`);
  process.exitCode = 1;
});
