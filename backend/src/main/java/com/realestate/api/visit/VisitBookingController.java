package com.realestate.api.visit;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class VisitBookingController {

    /** A visit that is still "open": asked for or confirmed, but not yet done or cancelled. */
    private static final List<VisitStatus> OPEN_STATUSES = List.of(VisitStatus.REQUESTED, VisitStatus.CONFIRMED);

    private final VisitBookingRepository visitBookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    /**
     * slotTime is an exact instant, so it must carry a zone: "2026-09-27T04:12:00Z"
     * or "2026-09-27T09:42:00+05:30". (The frontend converts the IST time the user
     * picked into this form.) Stored as UTC.
     */
    public record CreateVisitRequest(
            @NotBlank(message = "is required") String listingId,
            @NotNull(message = "is required") @Future(message = "must be in the future") Instant slotTime) {}

    @PostMapping("/api/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody CreateVisitRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing =
                listingRepository
                        .findById(request.listingId())
                        .orElseThrow(() -> new ListingNotFoundException(request.listingId()));

        if (listing.getStatus() != ListingStatus.LIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This listing isn't open for visit bookings right now.");
        }

        // Check-then-save, so two requests landing in the same instant could both pass;
        // MySQL has no "unique only while open" constraint. The frontend disables the
        // button while a request is in flight, which covers real-world double-clicks.
        if (visitBookingRepository.existsByListingIdAndTenantIdAndStatusIn(
                listing.getId(), principal.id(), OPEN_STATUSES)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a visit request for this property. We'll be in touch to confirm it.");
        }

        User tenant =
                userRepository
                        .findById(principal.id())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + principal.id()));

        visitBookingRepository.save(
                VisitBooking.builder().listing(listing).tenant(tenant).slotTime(request.slotTime()).build());
    }
}
