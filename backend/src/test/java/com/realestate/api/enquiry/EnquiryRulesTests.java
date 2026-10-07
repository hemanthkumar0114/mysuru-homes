package com.realestate.api.enquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** POST /api/enquiries - "I'm interested": LIVE listings only, one per tenant per listing. */
class EnquiryRulesTests extends ApiTestSupport {

    private record EnquiryRequest(String listingId) {}

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Test
    void theDatabaseItselfRejectsADuplicateEnquiry() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        Registered tenant = registerUser(UserRole.TENANT);
        String liveId = createLiveListing(owner, admin, Map.of());
        Listing listing = listingRepository.findById(liveId).orElseThrow();
        User tenantUser = userRepository.findById(tenant.id()).orElseThrow();

        enquiryRepository.save(Enquiry.builder().listing(listing).tenant(tenantUser).build());

        assertThatThrownBy(
                        () -> enquiryRepository.save(Enquiry.builder().listing(listing).tenant(tenantUser).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enquiringOnADraftListingIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String tenant = registerUser(UserRole.TENANT).token();
        String draftId = createListing(owner, Map.of()).id();

        ResponseEntity<ErrorBody> res =
                post("/api/enquiries", tenant, new EnquiryRequest(draftId), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().message()).contains("isn't open for enquiries");
    }

    @Test
    void enquiringWhileLoggedOutIsRejected() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String liveId = createLiveListing(owner, admin, Map.of());

        ResponseEntity<ErrorBody> res = post("/api/enquiries", null, new EnquiryRequest(liveId), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enquiringOnAnUnknownListingIs404() {
        String tenant = registerUser(UserRole.TENANT).token();

        ResponseEntity<ErrorBody> res =
                post("/api/enquiries", tenant, new EnquiryRequest("does-not-exist"), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aBlankListingIdIsRejectedWithAFriendlyMessage() {
        String tenant = registerUser(UserRole.TENANT).token();

        ResponseEntity<ErrorBody> res = post("/api/enquiries", tenant, new EnquiryRequest(""), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("Listing id");
    }

    @Test
    void enquiringOnALiveListingSucceeds() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());

        ResponseEntity<Void> res = post("/api/enquiries", tenant, new EnquiryRequest(liveId), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void theSameTenantCannotEnquireTwiceOnTheSameListing() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());

        post("/api/enquiries", tenant, new EnquiryRequest(liveId), Void.class);
        ResponseEntity<ErrorBody> second =
                post("/api/enquiries", tenant, new EnquiryRequest(liveId), ErrorBody.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().message()).contains("already told the owner");
    }

    @Test
    void aDifferentTenantCanStillEnquireOnTheSameListing() {
        String owner = registerUser(UserRole.OWNER).token();
        String admin = registerAdmin().token();
        String tenant1 = registerUser(UserRole.TENANT).token();
        String tenant2 = registerUser(UserRole.TENANT).token();
        String liveId = createLiveListing(owner, admin, Map.of());

        post("/api/enquiries", tenant1, new EnquiryRequest(liveId), Void.class);
        ResponseEntity<Void> res = post("/api/enquiries", tenant2, new EnquiryRequest(liveId), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
