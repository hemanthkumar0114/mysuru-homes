package com.realestate.api.enquiry;

import com.realestate.api.listing.ListingCount;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
    List<Enquiry> findByListingIdAndCreatedAtAfter(String listingId, Instant since);

    boolean existsByListingIdAndTenantId(String listingId, String tenantId);

    /** Enquiry totals for every listing an owner has, in a single query. */
    @Query(
            """
            SELECT new com.realestate.api.listing.ListingCount(e.listing.id, COUNT(e))
            FROM Enquiry e
            WHERE e.listing.owner.id = :ownerId
            GROUP BY e.listing.id
            """)
    List<ListingCount> countByOwner(@Param("ownerId") String ownerId);

    /** One listing's enquiries, newest first, with the tenant loaded (JOIN FETCH avoids lazy-loading errors). */
    @Query("SELECT e FROM Enquiry e JOIN FETCH e.tenant WHERE e.listing.id = :listingId ORDER BY e.createdAt DESC")
    List<Enquiry> findForListing(@Param("listingId") String listingId);
}
