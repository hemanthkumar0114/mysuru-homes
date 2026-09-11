package com.realestate.api.admin;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Everything here requires the ADMIN role (enforced in SecurityConfig, not per-method). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @GetMapping("/listings/pending")
    @Transactional(readOnly = true)
    public List<AdminListingSummary> pending() {
        return listingRepository.findByStatus(ListingStatus.DRAFT).stream()
                .map(AdminListingSummary::from)
                .toList();
    }

    /** Field team has physically visited and confirmed the listing - make it live. */
    @PostMapping("/listings/{id}/verify")
    @Transactional
    public AdminListingSummary verify(
            @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing = listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
        User admin =
                userRepository
                        .findById(principal.id())
                        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + principal.id()));

        listing.setStatus(ListingStatus.LIVE);
        listing.setVerifiedAt(Instant.now());
        listing.setVerifiedBy(admin);
        listing.setLastConfirmedAt(Instant.now());

        return AdminListingSummary.from(listingRepository.save(listing));
    }
}
