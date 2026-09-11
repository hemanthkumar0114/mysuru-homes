package com.realestate.api.listing;

import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
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
     * GET /api/listings                         -> all live listings
     * GET /api/listings?locality=Vijayanagar     -> filtered by locality
     * GET /api/listings?lat=..&lng=..&radiusKm=5 -> geo-radius search
     */
    @GetMapping("/api/listings")
    public List<ListingSummary> search(
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "5") Double radiusKm) {
        List<Listing> results;
        if (lat != null && lng != null) {
            results = listingRepository.findLiveWithinRadiusKm(lat, lng, radiusKm);
        } else if (locality != null) {
            results = listingRepository.findByStatusAndLocalityIgnoreCase(ListingStatus.LIVE, locality);
        } else {
            results = listingRepository.findByStatus(ListingStatus.LIVE);
        }
        return results.stream().map(ListingSummary::from).toList();
    }

    @GetMapping("/api/listings/{id}")
    public ListingSummary getOne(@PathVariable String id) {
        return listingRepository
                .findById(id)
                .map(ListingSummary::from)
                .orElseThrow(() -> new ListingNotFoundException(id));
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
