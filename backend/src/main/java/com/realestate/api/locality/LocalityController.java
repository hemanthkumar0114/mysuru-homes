package com.realestate.api.locality;

import com.realestate.api.common.Paging;
import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingPhotoRepository;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.listing.ListingSummary;
import com.realestate.api.listing.LocalityRent;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public locality landing pages (GET only, no login needed - see SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class LocalityController {

    private final LocalityPageRepository localityPageRepository;
    private final ListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;

    /**
     * All localities with their rent range and live-listing count - powers the home page section.
     * One query for the localities and one for every live listing's locality and rent,
     * grouped here, so the number of queries doesn't grow with the number of localities.
     */
    @GetMapping("/api/localities")
    @Transactional(readOnly = true)
    public List<LocalityView> list() {
        List<LocalityRent> liveRents = listingRepository.findLocalityRents(ListingStatus.LIVE);
        return localityPageRepository.findAllByOrderByLocalityNameAsc().stream()
                .map(page -> toView(page, ratesIn(page, liveRents), null))
                .toList();
    }

    /**
     * One locality, including its live listings, newest first. ?page=0&size=50 pages through them;
     * listingCount in the response is the total across all pages.
     */
    @GetMapping("/api/localities/{slug}")
    @Transactional(readOnly = true)
    public LocalityView get(
            @PathVariable String slug,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        LocalityPage localityPage =
                localityPageRepository.findById(slug).orElseThrow(() -> new LocalityNotFoundException(slug));
        Page<Listing> live =
                listingRepository.findLiveByLocalityPrefix(
                        ListingStatus.LIVE, localityPage.getLocalityName(), Paging.of(page, size));
        List<LocalityRent> rents = ratesIn(localityPage, listingRepository.findLocalityRents(ListingStatus.LIVE));
        return toView(localityPage, rents, withPhotos(live.getContent()));
    }

    /**
     * A listing belongs to a locality when its free-text locality is the locality's name, or the
     * name followed by a space and more text ("Vijayanagar 2nd Stage" counts under "Vijayanagar").
     * Same rule as ListingRepository.findLiveByLocalityPrefix, applied to the already-loaded rows.
     */
    private static List<LocalityRent> ratesIn(LocalityPage page, List<LocalityRent> liveRents) {
        String name = page.getLocalityName().toLowerCase(Locale.ROOT);
        return liveRents.stream()
                .filter(
                        r -> {
                            String locality = r.locality().toLowerCase(Locale.ROOT);
                            return locality.equals(name) || locality.startsWith(name + " ");
                        })
                .toList();
    }

    private LocalityView toView(LocalityPage page, List<LocalityRent> rents, List<ListingSummary> listings) {
        BigDecimal min = rents.stream().map(LocalityRent::rentAmount).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = rents.stream().map(LocalityRent::rentAmount).max(Comparator.naturalOrder()).orElse(null);

        return new LocalityView(
                page.getSlug(),
                page.getLocalityName(),
                page.getSeoContent(),
                page.getAvgRent(),
                min,
                max,
                rents.size(),
                listings);
    }

    private List<ListingSummary> withPhotos(List<Listing> listings) {
        Map<String, List<String>> photos =
                listingPhotoRepository.urlsByListingId(listings.stream().map(Listing::getId).toList());
        return listings.stream()
                .map(l -> ListingSummary.from(l, photos.getOrDefault(l.getId(), List.of())))
                .toList();
    }
}
