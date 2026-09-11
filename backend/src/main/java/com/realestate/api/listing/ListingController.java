package com.realestate.api.listing;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ListingController {

    private final ListingRepository listingRepository;

    /**
     * GET /api/listings                         -> all live listings
     * GET /api/listings?locality=Vijayanagar     -> filtered by locality
     * GET /api/listings?lat=..&lng=..&radiusKm=5 -> geo-radius search
     */
    @GetMapping("/api/listings")
    public List<ListingSummary> search(
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "5") Double radiusKm) {
        List<Listing> results;
        if (lat != null && lng != null) {
            results = listingRepository.findLiveWithinRadiusKm(lat, lng, radiusKm);
        } else if (locality != null) {
            results = listingRepository.findByStatusAndLocalityIgnoreCase(ListingStatus.LIVE, locality);
        } else {
            results = listingRepository.findByStatus(ListingStatus.LIVE);
        }
        return results.stream().map(ListingSummary::from).toList();
    }

    @GetMapping("/api/listings/{id}")
    public ListingSummary getOne(@PathVariable String id) {
        return listingRepository
                .findById(id)
                .map(ListingSummary::from)
                .orElseThrow(() -> new ListingNotFoundException(id));
    }
}
