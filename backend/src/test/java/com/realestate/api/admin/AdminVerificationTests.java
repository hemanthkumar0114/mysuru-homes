package com.realestate.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.listing.ListingStatus;
import com.realestate.api.listing.ListingSummary;
import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The admin moderation queue: who can see it, and what verifying a listing actually does. */
class AdminVerificationTests extends ApiTestSupport {

    @Test
    void aNonAdminCannotSeeThePendingQueue() {
        String owner = registerUser(UserRole.OWNER).token();
        String tenant = registerUser(UserRole.TENANT).token();

        assertThat(get("/api/admin/listings/pending", owner, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/listings/pending", tenant, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/listings/pending", null, ErrorBody.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void thePendingQueueOnlyListsDraftsAndIncludesTheOwnerAndAddress() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String draftId = createListing(owner, Map.of("title", unique("Pending house"), "addressLine", "42 Queue Road")).id();
        String liveId = createLiveListing(owner, admin, Map.of());

        ResponseEntity<AdminListingSummary[]> res =
                get("/api/admin/listings/pending", admin, AdminListingSummary[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AdminListingSummary> pending = List.of(res.getBody());
        assertThat(pending).extracting(AdminListingSummary::id).contains(draftId).doesNotContain(liveId);
        AdminListingSummary summary = pending.stream().filter(l -> l.id().equals(draftId)).findFirst().orElseThrow();
        assertThat(summary.addressLine()).isEqualTo("42 Queue Road");
        assertThat(summary.ownerName()).isNotBlank();
    }

    @Test
    void verifyingADraftMakesItLiveAndPubliclyVisible() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String draftId = createListing(owner, Map.of()).id();

        ResponseEntity<AdminListingSummary> verifyRes =
                post("/api/admin/listings/" + draftId + "/verify", admin, null, AdminListingSummary.class);
        assertThat(verifyRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ListingSummary> publicView = rest.getForEntity("/api/listings/" + draftId, ListingSummary.class);
        assertThat(publicView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicView.getBody().status()).isEqualTo(ListingStatus.LIVE);
        assertThat(publicView.getBody().verified()).isTrue();
    }

    @Test
    void verifyingIsAdminOnly() {
        String owner = registerUser(UserRole.OWNER).token();
        String draftId = createListing(owner, Map.of()).id();

        ResponseEntity<ErrorBody> res =
                post("/api/admin/listings/" + draftId + "/verify", owner, null, ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verifyingAnUnknownListingIs404() {
        String admin = registerAdmin().token();

        ResponseEntity<ErrorBody> res =
                post("/api/admin/listings/does-not-exist/verify", admin, null, ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aVerifiedListingNoLongerAppearsInThePendingQueue() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String draftId = createListing(owner, Map.of()).id();

        post("/api/admin/listings/" + draftId + "/verify", admin, null, AdminListingSummary.class);

        ResponseEntity<AdminListingSummary[]> res =
                get("/api/admin/listings/pending", admin, AdminListingSummary[].class);
        assertThat(res.getBody()).extracting(AdminListingSummary::id).doesNotContain(draftId);
    }
}
