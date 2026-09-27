package com.realestate.api.visit;

import java.time.Instant;

/**
 * A visit request as an admin sees it. Includes the tenant's name and email
 * because admins are the ones who contact people to confirm the slot.
 */
public record AdminVisitView(
        String id,
        String listingId,
        String listingTitle,
        String locality,
        String tenantName,
        String tenantEmail,
        Instant slotTime,
        VisitStatus status,
        Instant createdAt) {

    public static AdminVisitView from(VisitBooking visit) {
        return new AdminVisitView(
                visit.getId(),
                visit.getListing().getId(),
                visit.getListing().getTitle(),
                visit.getListing().getLocality(),
                visit.getTenant().getName(),
                visit.getTenant().getEmail(),
                visit.getSlotTime(),
                visit.getStatus(),
                visit.getCreatedAt());
    }
}
