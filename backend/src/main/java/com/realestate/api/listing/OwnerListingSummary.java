package com.realestate.api.listing;

import java.math.BigDecimal;

/** Same idea as ListingSummary, but for the owner's own "My listings" page - includes status. */
public record OwnerListingSummary(
        String id,
        String title,
        String locality,
        ListingType type,
        ListingStatus status,
        BigDecimal rentAmount,
        boolean verified) {

    public static OwnerListingSummary from(Listing listing) {
        return new OwnerListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getLocality(),
                listing.getType(),
                listing.getStatus(),
                listing.getRentAmount(),
                listing.getVerifiedAt() != null);
    }
}
