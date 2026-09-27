package com.realestate.api.visit;

import java.time.Instant;

/** A visit request as the tenant who made it sees it. */
public record VisitView(
        String id,
        String listingId,
        String listingTitle,
        String locality,
        Instant slotTime,
        VisitStatus status,
        Instant createdAt) {

    public static VisitView from(VisitBooking visit) {
        return new VisitView(
                visit.getId(),
                visit.getListing().getId(),
                visit.getListing().getTitle(),
                visit.getListing().getLocality(),
                visit.getSlotTime(),
                visit.getStatus(),
                visit.getCreatedAt());
    }
}
