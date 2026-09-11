package com.realestate.api.visit;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
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

    public record CreateVisitRequest(@NotBlank String listingId, @NotNull Instant slotTime) {}

    @PostMapping("/api/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @RequestBody CreateVisitRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
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
