package com.realestate.api.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.admin.AdminListingSummary;
import com.realestate.api.locality.LocalityPage;
import com.realestate.api.locality.LocalityPageRepository;
import com.realestate.api.locality.LocalityView;
import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ListingValidationAndPagingTests extends ApiTestSupport {

    @Autowired private LocalityPageRepository localityPageRepository;

    private String owner;
    private String admin;

    @BeforeEach
    void setUp() {
        owner = registerUser(UserRole.OWNER).token();
        admin = registerAdmin().token();
    }

    private ResponseEntity<ErrorBody> createWith(Map<String, Object> overrides) {
        return post("/api/listings", owner, listingRequest(overrides), ErrorBody.class);
    }

    private void assertRejected(Map<String, Object> overrides, String messagePart) {
        ResponseEntity<ErrorBody> res = createWith(overrides);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains(messagePart);
    }

    private void assertSearchRejected(String query, String messagePart) {
        ResponseEntity<ErrorBody> res = rest.getForEntity("/api/listings" + query, ErrorBody.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains(messagePart);
    }

    @Test
    void latitudeAndLongitudeMustBeRealCoordinates() {
        assertRejected(Map.of("lat", 91.0), "between -90 and 90");
        assertRejected(Map.of("lat", -91.0), "between -90 and 90");
        assertRejected(Map.of("lng", 181.0), "between -180 and 180");
        assertRejected(Map.of("lng", -181.0), "between -180 and 180");
    }

    @Test
    void roomCountsMustBeBetweenZeroAndTwenty() {
        assertRejected(Map.of("bedrooms", 21), "between 0 and 20");
        assertRejected(Map.of("bedrooms", -1), "between 0 and 20");
        assertRejected(Map.of("bathrooms", 21), "between 0 and 20");
    }

    @Test
    void textFieldsHaveALengthLimit() {
        assertRejected(Map.of("title", "x".repeat(151)), "150");
        assertRejected(Map.of("addressLine", "x".repeat(256)), "255");
        assertRejected(Map.of("locality", "x".repeat(101)), "100");
    }

    @Test
    void rentMustBeSaneAndHaveAtMostTwoDecimals() {
        assertRejected(Map.of("rentAmount", new BigDecimal("10000001")), "10,000,000");
        assertRejected(Map.of("rentAmount", new BigDecimal("10.123")), "2 decimal places");
    }

    @Test
    void aListingOnTheBoundariesIsAccepted() {
        ResponseEntity<OwnerListingSummary> res =
                post(
                        "/api/listings",
                        owner,
                        listingRequest(
                                Map.of("lat", 90.0, "lng", -180.0, "bedrooms", 0, "bathrooms", 20, "rentAmount", new BigDecimal("10000000.50"))),
                        OwnerListingSummary.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void searchRejectsOutOfRangeParameters() {
        assertSearchRejected("?lat=91&lng=76", "Latitude");
        assertSearchRejected("?lat=12&lng=181", "Longitude");
        assertSearchRejected("?lat=12.3", "together");
        assertSearchRejected("?lng=76.6", "together");
        assertSearchRejected("?lat=12.3&lng=76.6&radiusKm=0", "Radius");
        assertSearchRejected("?lat=12.3&lng=76.6&radiusKm=101", "Radius");
        assertSearchRejected("?bedrooms=99", "Bedrooms");
        assertSearchRejected("?minRent=-5", "negative");
        assertSearchRejected("?locality=" + "x".repeat(101), "Locality");
    }

    @Test
    void pageAndSizeAreValidated() {
        assertSearchRejected("?size=0", "Size");
        assertSearchRejected("?size=101", "Size");
        assertSearchRejected("?page=-1", "Page");
        assertThat(get("/api/admin/listings/pending?size=0", admin, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void searchIsPagedAndReportsTheTotalInAHeader() {
        String locality = unique("Pagetown");
        List<String> created = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            created.add(createLiveListing(owner, admin, Map.of("locality", locality)));
        }

        ResponseEntity<ListingSummary[]> first =
                rest.getForEntity("/api/listings?locality=" + locality + "&size=2", ListingSummary[].class);
        ResponseEntity<ListingSummary[]> second =
                rest.getForEntity("/api/listings?locality=" + locality + "&size=2&page=1", ListingSummary[].class);
        ResponseEntity<ListingSummary[]> beyond =
                rest.getForEntity("/api/listings?locality=" + locality + "&size=2&page=5", ListingSummary[].class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getHeaders().getFirst("X-Total-Count")).isEqualTo("3");
        assertThat(first.getBody()).hasSize(2);
        assertThat(second.getHeaders().getFirst("X-Total-Count")).isEqualTo("3");
        assertThat(second.getBody()).hasSize(1);
        assertThat(beyond.getBody()).isEmpty();

        List<String> seen = new ArrayList<>();
        for (ListingSummary l : first.getBody()) {
            seen.add(l.id());
        }
        for (ListingSummary l : second.getBody()) {
            seen.add(l.id());
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(created);
    }

    @Test
    void radiusSearchFindsAListingExactlyAtTheSearchPointAndSkipsDistantOnes() {
        String locality = unique("Geotown");
        String here = createLiveListing(owner, admin, Map.of("locality", locality, "lat", 12.34, "lng", 76.62));
        String far = createLiveListing(owner, admin, Map.of("locality", locality, "lat", 13.5, "lng", 77.5));

        ResponseEntity<ListingSummary[]> res =
                rest.getForEntity(
                        "/api/listings?lat=12.34&lng=76.62&radiusKm=1&locality=" + locality, ListingSummary[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
        assertThat(res.getBody()).extracting(ListingSummary::id).containsExactly(here).doesNotContain(far);
    }

    @Test
    void adminPendingQueueIsPaged() {
        createListing(owner, Map.of());
        createListing(owner, Map.of());

        ResponseEntity<AdminListingSummary[]> res =
                get("/api/admin/listings/pending?size=1", admin, AdminListingSummary[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(1);
        assertThat(Long.parseLong(res.getHeaders().getFirst("X-Total-Count"))).isGreaterThanOrEqualTo(2);
    }

    @Test
    void localityPagesCountTheirOwnListingsAndPageTheDetailView() {
        String name = unique("Zonetown");
        String slug = unique("slug");
        localityPageRepository.save(LocalityPage.builder().slug(slug).localityName(name).build());

        createLiveListing(owner, admin, Map.of("locality", name, "rentAmount", new BigDecimal("8000")));
        createLiveListing(owner, admin, Map.of("locality", name + " 2nd Stage", "rentAmount", new BigDecimal("12000")));
        createLiveListing(owner, admin, Map.of("locality", name + "x", "rentAmount", new BigDecimal("99000")));

        LocalityView fromList =
                List.of(rest.getForEntity("/api/localities", LocalityView[].class).getBody()).stream()
                        .filter(v -> v.slug().equals(slug))
                        .findFirst()
                        .orElseThrow();
        assertThat(fromList.listingCount()).isEqualTo(2);
        assertThat(fromList.minRent()).isEqualByComparingTo("8000");
        assertThat(fromList.maxRent()).isEqualByComparingTo("12000");
        assertThat(fromList.listings()).isNull();

        LocalityView detail = rest.getForEntity("/api/localities/" + slug + "?size=1", LocalityView.class).getBody();
        assertThat(detail.listingCount()).isEqualTo(2);
        assertThat(detail.listings()).hasSize(1);
    }
}
