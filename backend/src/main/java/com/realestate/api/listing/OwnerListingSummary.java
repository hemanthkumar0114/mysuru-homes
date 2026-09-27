package com.realestate.api.listing;

import java.math.BigDecimal;

/**
 * Same idea as ListingSummary, but for the owner's own "My listings" page - includes
 * status and how many enquiries / visit requests the listing has received.
 */
public record OwnerListingSummary(
        String id,
        String title,
        String locality,
        ListingType type,
        ListingStatus status,
        BigDecimal rentAmount,
        boolean verified,
        long enquiryCount,
        long visitCount) {

    /** For a listing that has no activity yet (e.g. one that was just created). */
    public static OwnerListingSummary from(Listing listing) {
        return from(listing, 0, 0);
    }

    public static OwnerListingSummary from(Listing listing, long enquiryCount, long visitCount) {
        return new OwnerListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getLocality(),
                listing.getType(),
                listing.getStatus(),
                listing.getRentAmount(),
                listing.getVerifiedAt() != null,
                enquiryCount,
                visitCount);
    }
}
