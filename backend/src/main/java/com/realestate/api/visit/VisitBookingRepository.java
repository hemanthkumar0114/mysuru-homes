package com.realestate.api.visit;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitBookingRepository extends JpaRepository<VisitBooking, String> {
    List<VisitBooking> findByStatus(VisitStatus status);

    /** Spring Data builds this query from the method name: listing.id = ? AND tenant.id = ? AND status IN (...). */
    boolean existsByListingIdAndTenantIdAndStatusIn(
            String listingId, String tenantId, Collection<VisitStatus> statuses);
}
