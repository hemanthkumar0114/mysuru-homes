package com.realestate.api.enquiry;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class EnquiryController {

    private final EnquiryRepository enquiryRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public record CreateEnquiryRequest(@NotBlank(message = "is required") String listingId) {}

    /**
     * Any logged-in user can enquire - the tenant is taken from the JWT, not the request body.
     * Rules: the listing must be LIVE, and each person can enquire once per listing.
     * (Checked in code rather than with a unique DB constraint because the table already
     * holds duplicate rows from before this rule existed.)
     */
    @PostMapping("/api/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody CreateEnquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing =
                listingRepository
                        .findById(request.listingId())
                        .orElseThrow(() -> new ListingNotFoundException(request.listingId()));

        if (listing.getStatus() != ListingStatus.LIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This listing isn't open for enquiries right now.");
        }

        if (enquiryRepository.existsByListingIdAndTenantId(listing.getId(), principal.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You've already told the owner you're interested in this property.");
        }

        User tenant =
                userRepository
                        .findById(principal.id())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + principal.id()));

        enquiryRepository.save(Enquiry.builder().listing(listing).tenant(tenant).build());
    }
}
