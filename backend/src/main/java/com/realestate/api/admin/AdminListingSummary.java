package com.realestate.api.admin;

import com.realestate.api.listing.Listing;
import java.math.BigDecimal;

/** What the moderation queue shows - includes the owner's name, which tenants never see here. */
public record AdminListingSummary(
        String id, String title, String locality, String ownerName, BigDecimal rentAmount) {

    public static AdminListingSummary from(Listing listing) {
        return new AdminListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getLocality(),
                listing.getOwner().getName(),
                listing.getRentAmount());
    }
}
