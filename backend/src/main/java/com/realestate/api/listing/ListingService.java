package com.realestate.api.listing;

import com.realestate.api.common.Paging;
import com.realestate.api.enquiry.EnquiryRepository;
import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import com.realestate.api.visit.VisitBookingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListingService {

    private static final int MAX_LOCALITY_LENGTH = 100;
    private static final int MAX_ROOMS = 20;
    private static final double MAX_RADIUS_KM = 100;

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EnquiryRepository enquiryRepository;
    private final VisitBookingRepository visitBookingRepository;
    private final ListingPhotoRepository listingPhotoRepository;

    @Transactional(readOnly = true)
    public Page<ListingSummary> search(
            String locality,
            ListingType type,
            BigDecimal minRent,
            BigDecimal maxRent,
            Integer bedrooms,
            Double lat,
            Double lng,
            Double radiusKm,
            Integer page,
            Integer size) {
        String localityFilter = StringUtils.hasText(locality) ? locality.trim() : null;
        validateSearch(localityFilter, minRent, maxRent, bedrooms, lat, lng, radiusKm);
        Pageable pageable = Paging.of(page, size);

        Page<Listing> results;
        if (lat != null && lng != null) {
            String typeName = type != null ? type.name() : null;
            results =
                    listingRepository.findLiveWithinRadiusKm(
                            lat, lng, radiusKm, localityFilter, typeName, minRent, maxRent, bedrooms, pageable);
        } else {
            results =
                    listingRepository.search(
                            ListingStatus.LIVE, localityFilter, type, minRent, maxRent, bedrooms, pageable);
        }
        Map<String, List<String>> photos =
                listingPhotoRepository.urlsByListingId(
                        results.getContent().stream().map(Listing::getId).toList());
        return results.map(l -> ListingSummary.from(l, photos.getOrDefault(l.getId(), List.of())));
    }

    private static void validateSearch(
            String locality,
            BigDecimal minRent,
            BigDecimal maxRent,
            Integer bedrooms,
            Double lat,
            Double lng,
            Double radiusKm) {
        if (locality != null && locality.length() > MAX_LOCALITY_LENGTH) {
            throw badRequest("Locality must be " + MAX_LOCALITY_LENGTH + " characters or fewer.");
        }
        if (minRent != null && minRent.signum() < 0 || maxRent != null && maxRent.signum() < 0) {
            throw badRequest("Rent cannot be negative.");
        }
        if (minRent != null && maxRent != null && minRent.compareTo(maxRent) > 0) {
            throw badRequest("Minimum rent cannot be greater than maximum rent.");
        }
        if (bedrooms != null && (bedrooms < 0 || bedrooms > MAX_ROOMS)) {
            throw badRequest("Bedrooms must be between 0 and " + MAX_ROOMS + ".");
        }
        if ((lat == null) != (lng == null)) {
            throw badRequest("Latitude and longitude must be given together.");
        }
        if (lat != null) {
            if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
                throw badRequest("Latitude must be between -90 and 90.");
            }
            if (!Double.isFinite(lng) || lng < -180 || lng > 180) {
                throw badRequest("Longitude must be between -180 and 180.");
            }
            if (radiusKm == null || !Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
                throw badRequest("Radius must be more than 0 and at most " + MAX_RADIUS_KM + " km.");
            }
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * A DRAFT (or EXPIRED) listing is visible only to its owner and to admins; everyone else
     * gets the same 404 as a listing that doesn't exist, so the API never confirms that a
     * hidden listing is there. The viewer is null for anonymous callers.
     */
    @Transactional(readOnly = true)
    public ListingSummary getOne(String id, AuthenticatedUser viewer) {
        Listing listing = listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
        if (listing.getStatus() != ListingStatus.LIVE && !canSeeHidden(listing, viewer)) {
            throw new ListingNotFoundException(id);
        }
        List<String> photos =
                listingPhotoRepository.findByListingIdOrderBySortOrderAsc(id).stream()
                        .map(ListingPhoto::getUrl)
                        .toList();
        return ListingSummary.from(listing, photos);
    }

    @Transactional
    public OwnerListingSummary create(CreateListingRequest request, String ownerId) {
        User owner = userRepository.requireById(ownerId);

        Listing listing =
                listingRepository.save(
                        Listing.builder()
                                .owner(owner)
                                .type(request.type())
                                .status(ListingStatus.DRAFT)
                                .title(request.title())
                                .addressLine(request.addressLine())
                                .locality(request.locality())
                                .lat(request.lat())
                                .lng(request.lng())
                                .rentAmount(request.rentAmount())
                                .bedrooms(request.bedrooms())
                                .bathrooms(request.bathrooms())
                                .build());

        return OwnerListingSummary.from(listing);
    }

    @Transactional(readOnly = true)
    public List<OwnerListingSummary> myListings(String ownerId) {
        Map<String, Long> enquiries = countsById(enquiryRepository.countByOwner(ownerId));
        Map<String, Long> visits = countsById(visitBookingRepository.countByOwner(ownerId));
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(l -> OwnerListingSummary.from(l, enquiries.getOrDefault(l.getId(), 0L), visits.getOrDefault(l.getId(), 0L)))
                .toList();
    }

    private static boolean canSeeHidden(Listing listing, AuthenticatedUser viewer) {
        if (viewer == null) {
            return false;
        }
        return UserRole.ADMIN.name().equals(viewer.role()) || listing.getOwner().getId().equals(viewer.id());
    }

    private static Map<String, Long> countsById(List<ListingCount> counts) {
        return counts.stream().collect(Collectors.toMap(ListingCount::listingId, ListingCount::count));
    }
}
