package com.realestate.api.enquiry;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private static final String ALREADY_ENQUIRED = "You've already told the owner you're interested in this property.";

    private final EnquiryRepository enquiryRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    /**
     * A tenant can enquire once per LIVE listing. The unique constraint on (listing, tenant)
     * is the real guarantee; the exists check just gives the friendly answer in the common case.
     */
    @Transactional
    public void create(String listingId, String tenantId) {
        Listing listing =
                listingRepository.findById(listingId).orElseThrow(() -> new ListingNotFoundException(listingId));

        if (listing.getStatus() != ListingStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing isn't open for enquiries right now.");
        }

        if (enquiryRepository.existsByListingIdAndTenantId(listing.getId(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ALREADY_ENQUIRED);
        }

        User tenant = userRepository.requireById(tenantId);

        try {
            enquiryRepository.saveAndFlush(Enquiry.builder().listing(listing).tenant(tenant).build());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ALREADY_ENQUIRED);
        }
    }
}
