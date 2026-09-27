package com.realestate.api.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, String> {

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<Listing> findByIdAndOwnerId(String id, String ownerId);

    /**
     * Listings whose locality text names this area, case-insensitive: an exact match
     * ("Vijayanagar") or the name followed by a space and more text ("Vijayanagar 2nd
     * Stage"). A plain substring/prefix match would also catch an unrelated area whose
     * name happens to start the same way (e.g. "Hebballi" for "Hebbal"), which this avoids.
     */
    @Query(
            """
            SELECT l FROM Listing l
            WHERE l.status = :status
              AND (LOWER(l.locality) = LOWER(:name) OR LOWER(l.locality) LIKE LOWER(CONCAT(:name, ' %')))
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findLiveByLocalityPrefix(@Param("status") ListingStatus status, @Param("name") String name);

    /**
     * One query for every filter combination: each "(:x IS NULL OR ...)" line
     * switches itself off when the caller passed null for that filter.
     */
    @Query(
            """
            SELECT l FROM Listing l
            WHERE l.status = :status
              AND (:locality IS NULL OR LOWER(l.locality) = LOWER(:locality))
              AND (:type IS NULL OR l.type = :type)
              AND (:minRent IS NULL OR l.rentAmount >= :minRent)
              AND (:maxRent IS NULL OR l.rentAmount <= :maxRent)
              AND (:bedrooms IS NULL OR l.bedrooms >= :bedrooms)
            ORDER BY l.createdAt DESC
            """)
    List<Listing> search(
            @Param("status") ListingStatus status,
            @Param("locality") String locality,
            @Param("type") ListingType type,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("bedrooms") Integer bedrooms);

    /**
     * Simple Haversine radius search - plain lat/lng columns, no PostGIS yet.
     * Fine for the single-corridor Phase 1 volume (a few hundred listings);
     * revisit with PostGIS + hibernate-spatial once search moves beyond one city.
     * Takes the same optional filters as search(); type is the enum's name().
     */
    @Query(
            value =
                    """
                    SELECT * FROM listings l
                    WHERE l.status = 'LIVE'
                    AND (
                        6371 * acos(
                            cos(radians(:lat)) * cos(radians(l.lat)) *
                            cos(radians(l.lng) - radians(:lng)) +
                            sin(radians(:lat)) * sin(radians(l.lat))
                        )
                    ) <= :radiusKm
                    AND (:locality IS NULL OR LOWER(l.locality) = LOWER(:locality))
                    AND (:type IS NULL OR l.type = :type)
                    AND (:minRent IS NULL OR l.rent_amount >= :minRent)
                    AND (:maxRent IS NULL OR l.rent_amount <= :maxRent)
                    AND (:bedrooms IS NULL OR l.bedrooms >= :bedrooms)
                    ORDER BY l.created_at DESC
                    """,
            nativeQuery = true)
    List<Listing> findLiveWithinRadiusKm(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("locality") String locality,
            @Param("type") String type,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("bedrooms") Integer bedrooms);
}
