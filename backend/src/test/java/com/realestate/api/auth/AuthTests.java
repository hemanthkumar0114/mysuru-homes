package com.realestate.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthTests extends ApiTestSupport {

    @Test
    void registerReturnsATokenAndTheNewUser() {
        String email = unique("newuser") + "@test.local";
        ResponseEntity<AuthResponse> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of("name", "New Person", "email", email, "password", PASSWORD, "role", "TENANT"),
                        AuthResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().token()).isNotBlank();
        assertThat(res.getBody().user().email()).isEqualTo(email);
        assertThat(res.getBody().user().role()).isEqualTo(UserRole.TENANT);
    }

    @Test
    void registerRejectsSelfSignupAsAdmin() {
        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of(
                                "name", "Sneaky", "email", unique("sneaky") + "@test.local", "password", PASSWORD, "role", "ADMIN"),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("TENANT or OWNER");
    }

    @Test
    void registerRejectsAShortPassword() {
        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of("name", "Short Pw", "email", unique("shortpw") + "@test.local", "password", "abc", "role", "TENANT"),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("between 8 and 72 characters");
    }

    @Test
    void registerRejectsAPasswordLongerThan72Characters() {
        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of(
                                "name", "Long Pw",
                                "email", unique("longpw") + "@test.local",
                                "password", "a".repeat(73),
                                "role", "TENANT"),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("between 8 and 72 characters");
    }

    @Test
    void registerRejectsAPasswordLongerThan72Bytes() {
        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of(
                                "name", "Wide Pw",
                                "email", unique("widepw") + "@test.local",
                                "password", "é".repeat(40),
                                "role", "TENANT"),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("between 8 and 72 characters");
    }

    @Test
    void registerRejectsADuplicateEmail() {
        Registered first = registerUser(UserRole.TENANT);

        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/register",
                        Map.of("name", "Copycat", "email", first.email(), "password", PASSWORD, "role", "TENANT"),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().message()).contains("already exists").doesNotContain(first.email());
    }

    @Test
    void anAccountIsLockedAfterRepeatedFailedLogins() {
        Registered user = registerUser(UserRole.TENANT);
        Registered other = registerUser(UserRole.TENANT);
        Map<String, String> wrong = Map.of("email", user.email(), "password", "wrong-password");

        for (int attempt = 0; attempt < 5; attempt++) {
            ResponseEntity<ErrorBody> failed = rest.postForEntity("/api/auth/login", wrong, ErrorBody.class);
            assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<ErrorBody> locked =
                rest.postForEntity(
                        "/api/auth/login", Map.of("email", user.email(), "password", PASSWORD), ErrorBody.class);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(locked.getBody().message()).contains("Too many failed login attempts");

        ResponseEntity<AuthResponse> unaffected =
                rest.postForEntity(
                        "/api/auth/login", Map.of("email", other.email(), "password", PASSWORD), AuthResponse.class);
        assertThat(unaffected.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void aSuccessfulLoginClearsEarlierFailures() {
        Registered user = registerUser(UserRole.TENANT);

        for (int attempt = 0; attempt < 4; attempt++) {
            rest.postForEntity(
                    "/api/auth/login", Map.of("email", user.email(), "password", "wrong-password"), ErrorBody.class);
        }
        ResponseEntity<AuthResponse> success =
                rest.postForEntity(
                        "/api/auth/login", Map.of("email", user.email(), "password", PASSWORD), AuthResponse.class);
        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);

        for (int attempt = 0; attempt < 4; attempt++) {
            rest.postForEntity(
                    "/api/auth/login", Map.of("email", user.email(), "password", "wrong-password"), ErrorBody.class);
        }
        ResponseEntity<AuthResponse> stillAllowed =
                rest.postForEntity(
                        "/api/auth/login", Map.of("email", user.email(), "password", PASSWORD), AuthResponse.class);
        assertThat(stillAllowed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anUnknownRouteGetsAGenericNotFoundMessage() {
        String token = registerUser(UserRole.TENANT).token();

        ResponseEntity<ErrorBody> res = get("/api/no-such-endpoint", token, ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().message()).isEqualTo("We couldn't find what you were looking for.");
    }

    @Test
    void loginSucceedsWithTheRightPassword() {
        Registered user = registerUser(UserRole.OWNER);

        ResponseEntity<AuthResponse> res =
                rest.postForEntity("/api/auth/login", Map.of("email", user.email(), "password", PASSWORD), AuthResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().token()).isNotBlank();
    }

    @Test
    void loginFailsWithTheWrongPassword() {
        Registered user = registerUser(UserRole.TENANT);

        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/login", Map.of("email", user.email(), "password", "wrong-password"), ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Incorrect email or password");
    }

    @Test
    void loginFailsForAnUnknownEmail() {
        ResponseEntity<ErrorBody> res =
                rest.postForEntity(
                        "/api/auth/login",
                        Map.of("email", "nobody-" + unique("x") + "@test.local", "password", PASSWORD),
                        ErrorBody.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aProtectedEndpointRejectsAMissingToken() {
        ResponseEntity<ErrorBody> res = get("/api/my-listings", null, ErrorBody.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aProtectedEndpointRejectsAJunkToken() {
        ResponseEntity<ErrorBody> res = get("/api/my-listings", "not-a-real-token", ErrorBody.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
