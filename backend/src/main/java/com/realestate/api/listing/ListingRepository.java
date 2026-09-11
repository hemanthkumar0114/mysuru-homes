package com.realestate.api.listing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, String> {

    List<Listing> findByStatusAndLocalityIgnoreCase(ListingStatus status, String locality);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    /**
     * Simple Haversine radius search - plain lat/lng columns, no PostGIS yet.
     * Fine for the single-corridor Phase 1 volume (a few hundred listings);
     * revisit with PostGIS + hibernate-spatial once search moves beyond one city.
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
                    """,
            nativeQuery = true)
    List<Listing> findLiveWithinRadiusKm(
            @Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);
}
