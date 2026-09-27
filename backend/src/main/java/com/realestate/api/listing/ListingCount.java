package com.realestate.api.listing;

/** "Listing X has N of something" - the row shape for the grouped count queries. */
public record ListingCount(String listingId, long count) {}
