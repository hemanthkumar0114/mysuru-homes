package com.realestate.api.locality;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.listing.ListingSummary;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Public locality landing pages (GET only, no login needed - see SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class LocalityController {

    private final LocalityPageRepository localityPageRepository;
    private final ListingRepository listingRepository;

    /** All localities with their rent range and live-listing count - powers the home page section. */
    @GetMapping("/api/localities")
    @Transactional(readOnly = true)
    public List<LocalityView> list() {
        return localityPageRepository.findAllByOrderByLocalityNameAsc().stream()
                .map(page -> toView(page, false))
                .toList();
    }

    /** One locality, including the live listings there. */
    @GetMapping("/api/localities/{slug}")
    @Transactional(readOnly = true)
    public LocalityView get(@PathVariable String slug) {
        LocalityPage page =
                localityPageRepository.findById(slug).orElseThrow(() -> new LocalityNotFoundException(slug));
        return toView(page, true);
    }

    private LocalityView toView(LocalityPage page, boolean includeListings) {
        // A listing belongs to a locality when its free-text locality starts with the
        // locality's name, so "Vijayanagar 2nd Stage" is counted under "Vijayanagar".
        List<Listing> live =
                listingRepository.findLiveByLocalityPrefix(ListingStatus.LIVE, page.getLocalityName());

        BigDecimal min = live.stream().map(Listing::getRentAmount).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = live.stream().map(Listing::getRentAmount).max(Comparator.naturalOrder()).orElse(null);

        return new LocalityView(
                page.getSlug(),
                page.getLocalityName(),
                page.getSeoContent(),
                page.getAvgRent(),
                min,
                max,
                live.size(),
                includeListings ? live.stream().map(ListingSummary::from).toList() : null);
    }
}
