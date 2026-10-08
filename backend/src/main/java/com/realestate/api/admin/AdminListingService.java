package com.realestate.api.admin;

import com.realestate.api.common.Paging;
import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminListingSummary> pending(Integer page, Integer size) {
        Pageable pageable = Paging.of(page, size, Sort.by("createdAt").ascending());
        return listingRepository.findByStatus(ListingStatus.DRAFT, pageable).map(AdminListingSummary::from);
    }

    /** Field team has physically visited and confirmed the listing - make it live. */
    @Transactional
    public AdminListingSummary verify(String listingId, String adminId) {
        Listing listing =
                listingRepository.findById(listingId).orElseThrow(() -> new ListingNotFoundException(listingId));
        User admin = userRepository.requireById(adminId);

        Instant now = Instant.now();
        listing.setStatus(ListingStatus.LIVE);
        listing.setVerifiedAt(now);
        listing.setVerifiedBy(admin);
        listing.setLastConfirmedAt(now);

        return AdminListingSummary.from(listingRepository.save(listing));
    }
}
