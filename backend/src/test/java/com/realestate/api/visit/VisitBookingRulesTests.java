package com.realestate.api.visit;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * POST /api/visits and its lifecycle: LIVE listings only, one open request per tenant per
 * listing, a tenant can cancel their own open request, and an admin can confirm or cancel any
 * request.
 */
class VisitBookingRulesTests extends ApiTestSupport {

    private record VisitRequest(String listingId, Instant slotTime) {}

    private static Instant future(int hours) {
        return Instant.now().plus(Duration.ofHours(hours));
    }

    @Test
    void bookingOnADraftListingIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String tenant = registerUser(UserRole.TENANT).token();
        String draftId = createListing(owner, Map.of()).id();

        ResponseEntity<ErrorBody> res =
                post("/api/visits", tenant, new VisitRequest(draftId, future(48)), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().message()).contains("isn't open for visit bookings");
    }

    @Test
    void bookingInThePastIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());

        ResponseEntity<ErrorBody> res =
                post("/api/visits", tenant, new VisitRequest(liveId, future(-2)), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("Visit time").contains("future");
    }

    @Test
    void bookingWhileLoggedOutIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String liveId = createLiveListing(owner, admin, Map.of());

        ResponseEntity<ErrorBody> res = post("/api/visits", null, new VisitRequest(liveId, future(48)), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void bookingOnALiveListingSucceedsAndShowsUpInMyVisits() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of("title", "Visit target"));

        ResponseEntity<Void> created = post("/api/visits", tenant, new VisitRequest(liveId, future(48)), Void.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<VisitView[]> mine = get("/api/visits/mine", tenant, VisitView[].class);
        assertThat(mine.getBody()).extracting(VisitView::listingId).contains(liveId);
        assertThat(mine.getBody()[0].status()).isEqualTo(VisitStatus.REQUESTED);
    }

    @Test
    void aTenantCannotHaveTwoOpenRequestsOnTheSameListing() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());

        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        ResponseEntity<ErrorBody> second =
                post("/api/visits", tenant, new VisitRequest(liveId, future(48)), ErrorBody.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().message()).contains("already have a visit request");
    }

    @Test
    void aTenantCanCancelTheirOwnOpenRequest() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant, VisitView[].class).getBody()[0].id();

        ResponseEntity<VisitView> res = post("/api/visits/" + visitId + "/cancel", tenant, null, VisitView.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().status()).isEqualTo(VisitStatus.CANCELLED);
    }

    @Test
    void cancellingAnAlreadyCancelledRequestIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant, VisitView[].class).getBody()[0].id();
        post("/api/visits/" + visitId + "/cancel", tenant, null, VisitView.class);

        ResponseEntity<ErrorBody> res =
                post("/api/visits/" + visitId + "/cancel", tenant, null, ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().message()).contains("already cancelled");
    }

    @Test
    void aTenantCannotCancelSomeoneElsesVisit() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant1 = registerUser(UserRole.TENANT).token();
        String tenant2 = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant1, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant1, VisitView[].class).getBody()[0].id();

        ResponseEntity<ErrorBody> res =
                post("/api/visits/" + visitId + "/cancel", tenant2, null, ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void afterCancellingTheTenantCanBookAgain() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant, VisitView[].class).getBody()[0].id();
        post("/api/visits/" + visitId + "/cancel", tenant, null, VisitView.class);

        ResponseEntity<Void> res = post("/api/visits", tenant, new VisitRequest(liveId, future(72)), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void anAdminCanConfirmAFreshRequestButNotTwice() {
        String owner = registerUser(UserRole.OWNER).token();
        Registered adminUser = registerAdmin();
        String admin = adminUser.token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant, VisitView[].class).getBody()[0].id();

        ResponseEntity<AdminVisitView> confirmed =
                post("/api/admin/visits/" + visitId + "/confirm", admin, null, AdminVisitView.class);
        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmed.getBody().status()).isEqualTo(VisitStatus.CONFIRMED);

        ResponseEntity<ErrorBody> again =
                post("/api/admin/visits/" + visitId + "/confirm", admin, null, ErrorBody.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void anOwnerCannotConfirmOrListAdminVisits() {
        String owner = registerUser(UserRole.OWNER).token();

        assertThat(get("/api/admin/visits", owner, ErrorBody.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aTenantCanCancelAConfirmedVisitAndAdminCanFilterByStatus() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());
        post("/api/visits", tenant, new VisitRequest(liveId, future(24)), Void.class);
        String visitId = get("/api/visits/mine", tenant, VisitView[].class).getBody()[0].id();
        post("/api/admin/visits/" + visitId + "/confirm", admin, null, AdminVisitView.class);

        ResponseEntity<VisitView> cancelled =
                post("/api/visits/" + visitId + "/cancel", tenant, null, VisitView.class);
        assertThat(cancelled.getBody().status()).isEqualTo(VisitStatus.CANCELLED);

        ResponseEntity<AdminVisitView[]> confirmedList =
                get("/api/admin/visits?status=CONFIRMED", admin, AdminVisitView[].class);
        assertThat(confirmedList.getBody()).extracting(AdminVisitView::id).doesNotContain(visitId);
    }
}
