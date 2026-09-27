package com.realestate.api.listing;

import com.realestate.api.enquiry.EnquiryRepository;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.visit.VisitBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OwnerActivityController {

    private final ListingRepository listingRepository;
    private final EnquiryRepository enquiryRepository;
    private final VisitBookingRepository visitBookingRepository;

    /**
     * The enquiries and visit requests on one of the owner's own listings. Asking about
     * someone else's listing gets the same 404 as a listing that doesn't exist.
     * (SecurityConfig already limits /api/my-listings/** to the OWNER role; this is the
     * second check - "is it YOUR listing?" - which a role alone can't answer.)
     */
    @GetMapping("/api/my-listings/{id}/activity")
    @Transactional(readOnly = true)
    public ListingActivity activity(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing =
                listingRepository
                        .findByIdAndOwnerId(id, principal.id())
                        .orElseThrow(() -> new ListingNotFoundException(id));

        return new ListingActivity(
                listing.getId(),
                listing.getTitle(),
                enquiryRepository.findForListing(id).stream().map(ListingActivity.EnquiryItem::from).toList(),
                visitBookingRepository.findForListing(id).stream().map(ListingActivity.VisitItem::from).toList());
    }
}
