package com.realestate.api.listing;

import java.math.BigDecimal;

/** Flat response shape for search/list views - keeps entities out of the API surface. */
public record ListingSummary(
        String id,
        String title,
        String locality,
        ListingType type,
        BigDecimal rentAmount,
        Integer bedrooms,
        Double lat,
        Double lng,
        boolean verified,
        ListingStatus status) {

    public static ListingSummary from(Listing listing) {
        return new ListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getLocality(),
                listing.getType(),
                listing.getRentAmount(),
                listing.getBedrooms(),
                listing.getLat(),
                listing.getLng(),
                listing.getVerifiedAt() != null,
                listing.getStatus());
    }
}
