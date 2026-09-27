package com.realestate.api.visit;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisitBookingController {

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
        User tenant =
                userRepository
                        .findById(principal.id())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + principal.id()));

        visitBookingRepository.save(
                VisitBooking.builder().listing(listing).tenant(tenant).slotTime(request.slotTime()).build());
    }
}
