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
        assertThat(res.getBody().message()).contains("at least 6 characters");
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
        assertThat(res.getBody().message()).contains(first.email());
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
