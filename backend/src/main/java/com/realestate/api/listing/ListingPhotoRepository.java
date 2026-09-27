package com.realestate.api.listing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, String> {
    List<ListingPhoto> findByListingIdOrderBySortOrderAsc(String listingId);

    long countByListingId(String listingId);

    List<ListingPhoto> findByListingIdInOrderBySortOrderAsc(Collection<String> listingIds);

    /** Photo URLs for several listings at once, keyed by listing id - avoids one query per listing. */
    default Map<String, List<String>> urlsByListingId(Collection<String> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        return findByListingIdInOrderBySortOrderAsc(listingIds).stream()
                .collect(Collectors.groupingBy(
                        p -> p.getListing().getId(), Collectors.mapping(ListingPhoto::getUrl, Collectors.toList())));
    }
}
