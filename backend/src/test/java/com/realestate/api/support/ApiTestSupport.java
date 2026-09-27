package com.realestate.api.support;

import com.realestate.api.auth.AuthResponse;
import com.realestate.api.listing.OwnerListingSummary;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base class for the HTTP-level test suite. Each test spins up the real Spring context
 * (JWT auth, validation, exception handling, the database) on a random port and talks to
 * it exactly like the frontend does, over real HTTP - so these tests exercise the actual
 * security rules and controllers, not a mocked slice of them. The database behind it is
 * in-memory H2 (see src/test/resources/application.yml): fast, needs no setup, and never
 * touches the real MySQL data the app uses outside tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "test-pass-123";

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** {"message": "..."} - the shape GlobalExceptionHandler sends back for every expected error. */
    public record ErrorBody(String message) {}

    public record Registered(String token, String id, String email) {}

    /** A short, collision-proof value for names/emails/titles - many tests share one database. */
    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    /** Registers a fresh TENANT or OWNER through the real public endpoint. */
    protected Registered registerUser(UserRole role) {
        String email = unique("user") + "@test.local";
        Map<String, Object> body =
                Map.of("name", "Test User", "email", email, "password", PASSWORD, "role", role.name());
        AuthResponse auth = rest.postForEntity("/api/auth/register", body, AuthResponse.class).getBody();
        return new Registered(auth.token(), auth.user().id(), email);
    }

    /**
     * ADMIN accounts can't self-register (see AuthController - that's a deliberate rule, not
     * a gap), so this goes straight to the repository, then logs in through the real endpoint
     * to get a genuine JWT - exactly the token an admin would actually be using.
     */
    protected Registered registerAdmin() {
        String email = unique("admin") + "@test.local";
        User admin =
                userRepository.save(
                        User.builder()
                                .name("Test Admin")
                                .email(email)
                                .passwordHash(passwordEncoder.encode(PASSWORD))
                                .role(UserRole.ADMIN)
                                .build());
        AuthResponse auth =
                rest.postForEntity("/api/auth/login", Map.of("email", email, "password", PASSWORD), AuthResponse.class)
                        .getBody();
        return new Registered(auth.token(), admin.getId(), email);
    }

    /** A valid CreateListingRequest body with sensible defaults - pass overrides to change a few fields. */
    protected Map<String, Object> listingRequest(Map<String, Object> overrides) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "RENT");
        body.put("title", unique("Test listing"));
        body.put("addressLine", "1 Test Road");
        body.put("locality", "Hebbal");
        body.put("lat", 12.34);
        body.put("lng", 76.62);
        body.put("rentAmount", new BigDecimal("10000"));
        body.put("bedrooms", 2);
        body.put("bathrooms", 1);
        body.putAll(overrides);
        return body;
    }

    /** Creates a DRAFT listing as the given owner and returns it. */
    protected OwnerListingSummary createListing(String ownerToken, Map<String, Object> overrides) {
        return post("/api/listings", ownerToken, listingRequest(overrides), OwnerListingSummary.class).getBody();
    }

    /** Creates a listing and immediately verifies it, so it comes back LIVE. */
    protected String createLiveListing(String ownerToken, String adminToken, Map<String, Object> overrides) {
        String id = createListing(ownerToken, overrides).id();
        post("/api/admin/listings/" + id + "/verify", adminToken, null, Void.class);
        return id;
    }

    protected <T> ResponseEntity<T> get(String path, String token, Class<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), type);
    }

    protected <T> ResponseEntity<T> post(String path, String token, Object body, Class<T> type) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), type);
    }

    protected HttpHeaders headers(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (token != null) {
            httpHeaders.setBearerAuth(token);
        }
        return httpHeaders;
    }
}
