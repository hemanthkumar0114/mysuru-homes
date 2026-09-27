package com.realestate.api.listing;

import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

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
        if (minRent != null && maxRent != null && minRent.compareTo(maxRent) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum rent cannot be greater than maximum rent.");
        }
        // An empty text box arrives as "" - treat it the same as "not given".
        String localityFilter = StringUtils.hasText(locality) ? locality.trim() : null;

        List<Listing> results;
        if (lat != null && lng != null) {
            String typeName = type != null ? type.name() : null;
            results =
                    listingRepository.findLiveWithinRadiusKm(
                            lat, lng, radiusKm, localityFilter, typeName, minRent, maxRent, bedrooms);
        } else {
            results =
                    listingRepository.search(
                            ListingStatus.LIVE, localityFilter, type, minRent, maxRent, bedrooms);
        }
        return results.stream().map(ListingSummary::from).toList();
    }

    /**
     * Public for LIVE listings. A DRAFT (or EXPIRED) listing is visible only to its
     * owner and to admins; everyone else gets the same 404 as a listing that doesn't
     * exist, so the API never confirms that a hidden listing is there.
     * The route is public, but JwtAuthFilter still reads a token if one is sent,
     * so "principal" is filled in for logged-in callers and null for anonymous ones.
     */
    @GetMapping("/api/listings/{id}")
    @Transactional(readOnly = true)
    public ListingSummary getOne(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing = listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
        if (listing.getStatus() != ListingStatus.LIVE && !canSeeHidden(listing, principal)) {
            throw new ListingNotFoundException(id);
        }
        return ListingSummary.from(listing);
    }

    private static boolean canSeeHidden(Listing listing, AuthenticatedUser principal) {
        if (principal == null) {
            return false;
        }
        return UserRole.ADMIN.name().equals(principal.role())
                || listing.getOwner().getId().equals(principal.id());
    }

    /** Owner submits a new property. It starts as DRAFT until an admin verifies it. */
    @PostMapping("/api/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerListingSummary create(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        User owner =
                userRepository
                        .findById(principal.id())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + principal.id()));

        Listing listing =
                listingRepository.save(
                        Listing.builder()
                                .owner(owner)
                                .type(request.type())
                                .status(ListingStatus.DRAFT)
                                .title(request.title())
                                .addressLine(request.addressLine())
                                .locality(request.locality())
                                .lat(request.lat())
                                .lng(request.lng())
                                .rentAmount(request.rentAmount())
                                .bedrooms(request.bedrooms())
                                .bathrooms(request.bathrooms())
                                .build());

        return OwnerListingSummary.from(listing);
    }

    /** The logged-in owner's own listings, in every status (DRAFT/LIVE/EXPIRED). */
    @GetMapping("/api/my-listings")
    public List<OwnerListingSummary> myListings(@AuthenticationPrincipal AuthenticatedUser principal) {
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(principal.id()).stream()
                .map(OwnerListingSummary::from)
                .toList();
    }
}
