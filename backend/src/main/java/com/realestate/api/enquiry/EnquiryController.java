package com.realestate.api.enquiry;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryRepository enquiryRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public record CreateEnquiryRequest(@NotBlank String listingId, @NotBlank String tenantUserId) {}

    @PostMapping("/api/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody CreateEnquiryRequest request) {
        Listing listing =
                listingRepository
                        .findById(request.listingId())
                        .orElseThrow(() -> new ListingNotFoundException(request.listingId()));
        User tenant =
                userRepository
                        .findById(request.tenantUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + request.tenantUserId()));

        enquiryRepository.save(Enquiry.builder().listing(listing).tenant(tenant).build());
    }
}
