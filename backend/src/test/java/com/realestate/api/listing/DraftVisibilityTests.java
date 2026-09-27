package com.realestate.api.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * GET /api/listings/{id} for a DRAFT listing: only its owner and admins may see it. Everyone
 * else - including a logged-in stranger - gets the same 404 as an id that doesn't exist, so
 * the API never confirms a hidden listing is there.
 */
class DraftVisibilityTests extends ApiTestSupport {

    @Test
    void ownerCanSeeTheirOwnDraft() {
        String owner = registerUser(UserRole.OWNER).token();
        String id = createListing(owner, Map.of()).id();

        ResponseEntity<ListingSummary> res = get("/api/listings/" + id, owner, ListingSummary.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().status()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void anonymousVisitorsGet404OnADraft() {
        String owner = registerUser(UserRole.OWNER).token();
        String id = createListing(owner, Map.of()).id();

        assertThat(rest.getForEntity("/api/listings/" + id, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aDifferentOwnerGets404OnSomeoneElsesDraft() {
        String owner = registerUser(UserRole.OWNER).token();
        String otherOwner = registerUser(UserRole.OWNER).token();
        String id = createListing(owner, Map.of()).id();

        assertThat(get("/api/listings/" + id, otherOwner, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aTenantGets404OnADraft() {
        String owner = registerUser(UserRole.OWNER).token();
        String tenant = registerUser(UserRole.TENANT).token();
        String id = createListing(owner, Map.of()).id();

        assertThat(get("/api/listings/" + id, tenant, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anAdminCanSeeAnyDraft() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String id = createListing(owner, Map.of()).id();

        ResponseEntity<ListingSummary> res = get("/api/listings/" + id, admin, ListingSummary.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void onceVerifiedTheListingIsPubliclyVisible() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String id = createLiveListing(owner, admin, Map.of());

        ResponseEntity<ListingSummary> res = rest.getForEntity("/api/listings/" + id, ListingSummary.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().status()).isEqualTo(ListingStatus.LIVE);
        assertThat(res.getBody().verified()).isTrue();
    }

    @Test
    void anIdThatDoesNotExistIs404ForEveryone() {
        assertThat(rest.getForEntity("/api/listings/does-not-exist", ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
