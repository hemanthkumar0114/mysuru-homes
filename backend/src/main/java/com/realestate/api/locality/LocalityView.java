package com.realestate.api.locality;

import com.realestate.api.listing.ListingSummary;
import java.math.BigDecimal;
import java.util.List;

/**
 * What the locality pages show. typicalRent is the stored average for the area;
 * minRent / maxRent / listingCount are worked out from the LIVE listings there right now
 * (null when there are none). "listings" is only filled in for the single-locality page.
 */
public record LocalityView(
        String slug,
        String name,
        String description,
        BigDecimal typicalRent,
        BigDecimal minRent,
        BigDecimal maxRent,
        int listingCount,
        List<ListingSummary> listings) {}
