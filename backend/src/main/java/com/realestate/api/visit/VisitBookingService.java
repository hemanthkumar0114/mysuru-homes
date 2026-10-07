package com.realestate.api.visit;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingNotFoundException;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VisitBookingService {

    private static final List<VisitStatus> OPEN_STATUSES = List.of(VisitStatus.REQUESTED, VisitStatus.CONFIRMED);

    private final VisitBookingRepository visitBookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public void create(String listingId, Instant slotTime, String tenantId) {
        Listing listing =
                listingRepository.findById(listingId).orElseThrow(() -> new ListingNotFoundException(listingId));

        if (listing.getStatus() != ListingStatus.LIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This listing isn't open for visit bookings right now.");
        }

        // Check-then-save, so two requests landing in the same instant could both pass;
        // MySQL has no "unique only while open" constraint. The frontend disables the
        // button while a request is in flight, which covers real-world double-clicks.
        if (visitBookingRepository.existsByListingIdAndTenantIdAndStatusIn(listing.getId(), tenantId, OPEN_STATUSES)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a visit request for this property. We'll be in touch to confirm it.");
        }

        User tenant = userRepository.requireById(tenantId);

        visitBookingRepository.save(VisitBooking.builder().listing(listing).tenant(tenant).slotTime(slotTime).build());
    }

    @Transactional(readOnly = true)
    public List<VisitView> mine(String tenantId) {
        return visitBookingRepository.findForTenant(tenantId).stream().map(VisitView::from).toList();
    }

    @Transactional
    public VisitView cancelOwn(String visitId, String tenantId) {
        VisitBooking visit =
                visitBookingRepository
                        .findByIdForTenant(visitId, tenantId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "We couldn't find that visit request."));

        requireOpen(visit);
        visit.setStatus(VisitStatus.CANCELLED);
        return VisitView.from(visit);
    }

    @Transactional(readOnly = true)
    public List<AdminVisitView> listForAdmin(VisitStatus status) {
        return visitBookingRepository.findAllForAdmin(status).stream().map(AdminVisitView::from).toList();
    }

    @Transactional
    public AdminVisitView confirm(String visitId) {
        VisitBooking visit = findForAdmin(visitId);
        if (visit.getStatus() != VisitStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a new request can be confirmed. This one is " + visit.getStatus().name().toLowerCase() + ".");
        }
        visit.setStatus(VisitStatus.CONFIRMED);
        return AdminVisitView.from(visit);
    }

    @Transactional
    public AdminVisitView cancelAsAdmin(String visitId) {
        VisitBooking visit = findForAdmin(visitId);
        requireOpen(visit);
        visit.setStatus(VisitStatus.CANCELLED);
        return AdminVisitView.from(visit);
    }

    private VisitBooking findForAdmin(String visitId) {
        return visitBookingRepository
                .findByIdWithDetails(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "We couldn't find that visit request."));
    }

    private static void requireOpen(VisitBooking visit) {
        if (!visit.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This visit request is already " + visit.getStatus().name().toLowerCase() + ", so it can't be cancelled.");
        }
    }
}
