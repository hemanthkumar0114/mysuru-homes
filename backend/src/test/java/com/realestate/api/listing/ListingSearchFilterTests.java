package com.realestate.api.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** GET /api/listings - the public search endpoint and its filters. */
class ListingSearchFilterTests extends ApiTestSupport {

    private String owner;
    private String admin;
    // A distinct locality per test class run, so this class's counts can't be thrown off by
    // listings another test class (sharing the same in-memory database) happens to create.
    private final String locality = unique("Testville");

    private String rentalTwoBed;
    private String pgOneBed;
    private String draftListing;

    @BeforeEach
    void setUp() {
        owner = registerUser(UserRole.OWNER).token();
        admin = registerAdmin().token();

        rentalTwoBed =
                createLiveListing(
                        owner,
                        admin,
                        Map.of("type", "RENT", "locality", locality, "rentAmount", new BigDecimal("12000"), "bedrooms", 2));
        pgOneBed =
                createLiveListing(
                        owner,
                        admin,
                        Map.of("type", "PG", "locality", locality, "rentAmount", new BigDecimal("7000"), "bedrooms", 1));
        draftListing =
                createListing(
                                owner,
                                Map.of("type", "RENT", "locality", locality, "rentAmount", new BigDecimal("9000"), "bedrooms", 3))
                        .id();
    }

    private List<ListingSummary> search(String query) {
        ResponseEntity<ListingSummary[]> res = rest.getForEntity("/api/listings" + query, ListingSummary[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(res.getBody());
    }

    private static List<String> ids(List<ListingSummary> listings) {
        return listings.stream().map(ListingSummary::id).toList();
    }

    @Test
    void onlyLiveListingsAppearInSearch() {
        List<String> ids = ids(search("?locality=" + locality));
        assertThat(ids).contains(rentalTwoBed, pgOneBed).doesNotContain(draftListing);
    }

    @Test
    void localityFilterIsCaseInsensitive() {
        List<String> ids = ids(search("?locality=" + locality.toUpperCase()));
        assertThat(ids).contains(rentalTwoBed, pgOneBed);
    }

    @Test
    void typeFilterNarrowsToOneType() {
        List<ListingSummary> pgOnly = search("?locality=" + locality + "&type=PG");
        assertThat(ids(pgOnly)).containsExactly(pgOneBed);
        assertThat(pgOnly).allMatch(l -> l.type() == ListingType.PG);
    }

    @Test
    void rentRangeFilterKeepsOnlyListingsInsideIt() {
        List<String> ids = ids(search("?locality=" + locality + "&minRent=10000&maxRent=15000"));
        assertThat(ids).containsExactly(rentalTwoBed);
    }

    @Test
    void bedroomsFilterMeansAtLeastThatMany() {
        List<String> ids = ids(search("?locality=" + locality + "&bedrooms=2"));
        assertThat(ids).containsExactly(rentalTwoBed);
    }

    @Test
    void filtersCanBeCombined() {
        List<String> ids = ids(search("?locality=" + locality + "&type=RENT&minRent=10000"));
        assertThat(ids).containsExactly(rentalTwoBed);
    }

    @Test
    void aMinRentAboveMaxRentIsRejected() {
        ResponseEntity<ErrorBody> res =
                rest.getForEntity("/api/listings?minRent=20000&maxRent=10000", ErrorBody.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("Minimum rent");
    }

    @Test
    void anUnknownTypeValueIsRejected() {
        ResponseEntity<ErrorBody> res = rest.getForEntity("/api/listings?type=MANSION", ErrorBody.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void searchNeedsNoLogin() {
        // No Authorization header at all - GET /api/listings is public.
        assertThat(rest.getForEntity("/api/listings", ListingSummary[].class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
