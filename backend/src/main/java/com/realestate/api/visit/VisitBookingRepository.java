package com.realestate.api.visit;

import com.realestate.api.listing.ListingCount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBookingRepository extends JpaRepository<VisitBooking, String> {
    List<VisitBooking> findByStatus(VisitStatus status);

    /** Spring Data builds this query from the method name: listing.id = ? AND tenant.id = ? AND status IN (...). */
    boolean existsByListingIdAndTenantIdAndStatusIn(
            String listingId, String tenantId, Collection<VisitStatus> statuses);

    /** A tenant's own requests, soonest visit first. JOIN FETCH loads the listing in the same query. */
    @Query("SELECT v FROM VisitBooking v JOIN FETCH v.listing WHERE v.tenant.id = :tenantId ORDER BY v.slotTime DESC")
    List<VisitBooking> findForTenant(@Param("tenantId") String tenantId);

    /** Lookup that only succeeds for the tenant's own request, so nobody can touch someone else's. */
    @Query("SELECT v FROM VisitBooking v JOIN FETCH v.listing WHERE v.id = :id AND v.tenant.id = :tenantId")
    Optional<VisitBooking> findByIdForTenant(@Param("id") String id, @Param("tenantId") String tenantId);

    /** Everything, for the admin page. A null status means "all statuses". */
    @Query(
            """
            SELECT v FROM VisitBooking v JOIN FETCH v.listing JOIN FETCH v.tenant
            WHERE (:status IS NULL OR v.status = :status)
            ORDER BY v.createdAt DESC
            """)
    List<VisitBooking> findAllForAdmin(@Param("status") VisitStatus status);

    @Query("SELECT v FROM VisitBooking v JOIN FETCH v.listing JOIN FETCH v.tenant WHERE v.id = :id")
    Optional<VisitBooking> findByIdWithDetails(@Param("id") String id);

    @Query("SELECT v FROM VisitBooking v JOIN FETCH v.tenant WHERE v.listing.id = :listingId ORDER BY v.slotTime DESC")
    List<VisitBooking> findForListing(@Param("listingId") String listingId);

    /** Visit-request totals (every status) for every listing an owner has, in a single query. */
    @Query(
            """
            SELECT new com.realestate.api.listing.ListingCount(v.listing.id, COUNT(v))
            FROM VisitBooking v
            WHERE v.listing.owner.id = :ownerId
            GROUP BY v.listing.id
            """)
    List<ListingCount> countByOwner(@Param("ownerId") String ownerId);
}
