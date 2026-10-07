-- Run this ONCE against the real database before deploying this version.
-- The app now enforces one enquiry per tenant per listing with a unique
-- constraint (uk_enquiry_listing_tenant). Hibernate adds it on startup, but it
-- cannot add it while duplicate rows exist - it only logs an error and carries
-- on without the constraint. This keeps the earliest enquiry for each
-- (listing, tenant) pair and removes the later duplicates.

DELETE e1
FROM enquiries e1
JOIN enquiries e2
  ON e1.listing_id = e2.listing_id
 AND e1.tenant_id = e2.tenant_id
 AND (e1.created_at > e2.created_at
      OR (e1.created_at = e2.created_at AND e1.id > e2.id));
