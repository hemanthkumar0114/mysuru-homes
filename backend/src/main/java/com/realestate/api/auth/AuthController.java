package com.realestate.api.auth;

import com.realestate.api.security.JwtService;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        // Self-signup is only ever TENANT or OWNER. ADMIN and FIELD_AGENT
        // accounts are created directly (e.g. via DemoDataSeeder or, later,
        // an internal admin tool) - never through this public endpoint.
        if (request.role() != UserRole.TENANT && request.role() != UserRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-signup role must be TENANT or OWNER");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be between 8 and 72 characters");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user;
        try {
            user =
                    userRepository.save(
                            User.builder()
                                    .name(request.name())
                                    .email(request.email())
                                    .passwordHash(passwordEncoder.encode(request.password()))
                                    .role(request.role())
                                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }

        return new AuthResponse(jwtService.generateToken(user), UserView.from(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new TooManyLoginAttemptsException();
        }

        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.email());
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccess(request.email());
        return new AuthResponse(jwtService.generateToken(user), UserView.from(user));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException() {
            super("An account with that email already exists.");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Incorrect email or password");
        }
    }
}
