package com.realestate.api.admin;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the moderation queue shows. Includes the owner's name and the full
 * address - the field team needs both to visit - which the public listing
 * API never exposes.
 */
public record AdminListingSummary(
        String id,
        String title,
        String addressLine,
        String locality,
        ListingType type,
        Integer bedrooms,
        String ownerName,
        BigDecimal rentAmount,
        Instant createdAt) {

    public static AdminListingSummary from(Listing listing) {
        return new AdminListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getAddressLine(),
                listing.getLocality(),
                listing.getType(),
                listing.getBedrooms(),
                listing.getOwner().getName(),
                listing.getRentAmount(),
                listing.getCreatedAt());
    }
}
