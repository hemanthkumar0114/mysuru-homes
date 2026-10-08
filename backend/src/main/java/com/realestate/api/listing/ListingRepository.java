package com.realestate.api.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, String> {

    /** Owner is fetched in the same query because the admin queue shows the owner's name. */
    @EntityGraph(attributePaths = "owner")
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    List<Listing> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<Listing> findByIdAndOwnerId(String id, String ownerId);

    /**
     * Listings whose locality text names this area, case-insensitive: an exact match
     * ("Vijayanagar") or the name followed by a space and more text ("Vijayanagar 2nd
     * Stage"). A plain substring/prefix match would also catch an unrelated area whose
     * name happens to start the same way (e.g. "Hebballi" for "Hebbal"), which this avoids.
     */
    @Query(
            value =
                    """
                    SELECT l FROM Listing l
                    WHERE l.status = :status
                      AND (LOWER(l.locality) = LOWER(:name) OR LOWER(l.locality) LIKE LOWER(CONCAT(:name, ' %')))
                    ORDER BY l.createdAt DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(l) FROM Listing l
                    WHERE l.status = :status
                      AND (LOWER(l.locality) = LOWER(:name) OR LOWER(l.locality) LIKE LOWER(CONCAT(:name, ' %')))
                    """)
    Page<Listing> findLiveByLocalityPrefix(
            @Param("status") ListingStatus status, @Param("name") String name, Pageable pageable);

    /**
     * Just the locality text and rent of every listing in a status. The locality list page
     * groups these in memory, so it needs one query in total instead of one per locality.
     */
    @Query("SELECT new com.realestate.api.listing.LocalityRent(l.locality, l.rentAmount) FROM Listing l WHERE l.status = :status")
    List<LocalityRent> findLocalityRents(@Param("status") ListingStatus status);

    /**
     * One query for every filter combination: each "(:x IS NULL OR ...)" line
     * switches itself off when the caller passed null for that filter.
     */
    @Query(
            value =
                    """
                    SELECT l FROM Listing l
                    WHERE l.status = :status
                      AND (:locality IS NULL OR LOWER(l.locality) = LOWER(:locality))
                      AND (:type IS NULL OR l.type = :type)
                      AND (:minRent IS NULL OR l.rentAmount >= :minRent)
                      AND (:maxRent IS NULL OR l.rentAmount <= :maxRent)
                      AND (:bedrooms IS NULL OR l.bedrooms >= :bedrooms)
                    ORDER BY l.createdAt DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(l) FROM Listing l
                    WHERE l.status = :status
                      AND (:locality IS NULL OR LOWER(l.locality) = LOWER(:locality))
                      AND (:type IS NULL OR l.type = :type)
                      AND (:minRent IS NULL OR l.rentAmount >= :minRent)
                      AND (:maxRent IS NULL OR l.rentAmount <= :maxRent)
                      AND (:bedrooms IS NULL OR l.bedrooms >= :bedrooms)
                    """)
    Page<Listing> search(
            @Param("status") ListingStatus status,
            @Param("locality") String locality,
            @Param("type") ListingType type,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("bedrooms") Integer bedrooms,
            Pageable pageable);

    String WITHIN_RADIUS_FILTERS =
            """
            WHERE l.status = 'LIVE'
            AND (
                6371 * acos(LEAST(1.0, GREATEST(-1.0,
                    cos(radians(:lat)) * cos(radians(l.lat)) *
                    cos(radians(l.lng) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(l.lat))
                )))
            ) <= :radiusKm
            AND (:locality IS NULL OR LOWER(l.locality) = LOWER(:locality))
            AND (:type IS NULL OR l.type = :type)
            AND (:minRent IS NULL OR l.rent_amount >= :minRent)
            AND (:maxRent IS NULL OR l.rent_amount <= :maxRent)
            AND (:bedrooms IS NULL OR l.bedrooms >= :bedrooms)
            """;

    /**
     * Simple Haversine radius search - plain lat/lng columns, no PostGIS yet.
     * Fine for the single-corridor Phase 1 volume (a few hundred listings);
     * revisit with PostGIS + hibernate-spatial once search moves beyond one city.
     * Takes the same optional filters as search(); type is the enum's name().
     * LEAST/GREATEST keep the acos input inside [-1, 1]: rounding can push it just past 1
     * for a listing exactly at the search point, and acos would then return NULL and drop it.
     */
    @Query(
            value = "SELECT * FROM listings l " + WITHIN_RADIUS_FILTERS + " ORDER BY l.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM listings l " + WITHIN_RADIUS_FILTERS,
            nativeQuery = true)
    Page<Listing> findLiveWithinRadiusKm(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("locality") String locality,
            @Param("type") String type,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("bedrooms") Integer bedrooms,
            Pageable pageable);
}
