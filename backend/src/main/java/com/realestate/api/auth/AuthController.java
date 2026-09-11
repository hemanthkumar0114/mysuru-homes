package com.realestate.api.auth;

import com.realestate.api.security.JwtService;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        // Self-signup is only ever TENANT or OWNER. ADMIN and FIELD_AGENT
        // accounts are created directly (e.g. via DemoDataSeeder or, later,
        // an internal admin tool) - never through this public endpoint.
        if (request.role() != UserRole.TENANT && request.role() != UserRole.OWNER) {
            throw new IllegalArgumentException("Self-signup role must be TENANT or OWNER");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user =
                userRepository.save(
                        User.builder()
                                .name(request.name())
                                .email(request.email())
                                .passwordHash(passwordEncoder.encode(request.password()))
                                .role(request.role())
                                .build());

        return new AuthResponse(jwtService.generateToken(user), UserView.from(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user), UserView.from(user));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException(String email) {
            super("An account already exists for " + email);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Incorrect email or password");
        }
    }
}
