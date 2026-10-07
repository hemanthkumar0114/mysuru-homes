package com.realestate.api.listing;

import com.realestate.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    /**
     * GET /api/listings                          -> all live listings, newest first
     * GET /api/listings?locality=Vijayanagar      -> only that locality
     * GET /api/listings?type=PG                   -> RENT or PG
     * GET /api/listings?minRent=8000&maxRent=15000 -> rent range (either end optional)
     * GET /api/listings?bedrooms=2                -> at least 2 bedrooms
     * GET /api/listings?lat=..&lng=..&radiusKm=5  -> geo-radius search
     * Every filter is optional and they can be combined.
     */
    @GetMapping("/api/listings")
    public List<ListingSummary> search(
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) ListingType type,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "5") Double radiusKm) {
        return listingService.search(locality, type, minRent, maxRent, bedrooms, lat, lng, radiusKm);
    }

    /**
     * Public for LIVE listings; DRAFT and EXPIRED ones are visible only to their owner and admins.
     * The route is public, but JwtAuthFilter still reads a token if one is sent, so "principal"
     * is filled in for logged-in callers and null for anonymous ones.
     */
    @GetMapping("/api/listings/{id}")
    public ListingSummary getOne(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return listingService.getOne(id, principal);
    }

    /** Owner submits a new property. It starts as DRAFT until an admin verifies it. */
    @PostMapping("/api/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerListingSummary create(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return listingService.create(request, principal.id());
    }

    /** The logged-in owner's own listings, in every status (DRAFT/LIVE/EXPIRED). */
    @GetMapping("/api/my-listings")
    public List<OwnerListingSummary> myListings(@AuthenticationPrincipal AuthenticatedUser principal) {
        return listingService.myListings(principal.id());
    }
}
