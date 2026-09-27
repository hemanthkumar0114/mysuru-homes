package com.realestate.api.admin;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.visit.AdminVisitView;
import com.realestate.api.visit.VisitBooking;
import com.realestate.api.visit.VisitBookingRepository;
import com.realestate.api.visit.VisitStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Everything here requires the ADMIN role (enforced in SecurityConfig, not per-method). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final VisitBookingRepository visitBookingRepository;

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

    /** Every visit request, newest first. ?status=REQUESTED narrows it to one status. */
    @GetMapping("/visits")
    @Transactional(readOnly = true)
    public List<AdminVisitView> visits(@RequestParam(required = false) VisitStatus status) {
        return visitBookingRepository.findAllForAdmin(status).stream().map(AdminVisitView::from).toList();
    }

    /** Only a fresh request (REQUESTED) can be confirmed. */
    @PostMapping("/visits/{id}/confirm")
    @Transactional
    public AdminVisitView confirmVisit(@PathVariable String id) {
        VisitBooking visit = findVisit(id);
        if (visit.getStatus() != VisitStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a new request can be confirmed. This one is " + visit.getStatus().name().toLowerCase() + ".");
        }
        visit.setStatus(VisitStatus.CONFIRMED);
        return AdminVisitView.from(visit);
    }

    /** A request that is still open (new or confirmed) can be cancelled. */
    @PostMapping("/visits/{id}/cancel")
    @Transactional
    public AdminVisitView cancelVisit(@PathVariable String id) {
        VisitBooking visit = findVisit(id);
        if (!visit.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This visit request is already " + visit.getStatus().name().toLowerCase() + ", so it can't be cancelled.");
        }
        visit.setStatus(VisitStatus.CANCELLED);
        return AdminVisitView.from(visit);
    }

    private VisitBooking findVisit(String id) {
        return visitBookingRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "We couldn't find that visit request."));
    }
}
